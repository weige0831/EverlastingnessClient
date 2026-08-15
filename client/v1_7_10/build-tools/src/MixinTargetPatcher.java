import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Post-reobf patcher for the MC 1.7.10 client jar.
 *
 * <p>MC 1.7.10 ships <b>fully notch-obfuscated</b> (class names <em>and</em>
 * method names <em>and</em> field names). RFG's {@code reobfJar} only remaps to
 * SRG (func_N/field_N), leaving all references in our mixins in MCP/SRG form,
 * which the runtime cannot resolve. This patcher performs a full SRG->notch
 * reobfuscation pass using ASM's {@link ClassRemapper} driven by maps parsed
 * from Forge's {@code notch-srg.srg}.</p>
 *
 * <p>It does three things:</p>
 * <ol>
 *   <li><b>Bytecode remap</b>: rewrites every class reference (descriptors,
 *       method bodies, annotations) to its notch name via a
 *       {@code Remapper.map} override.</li>
 *   <li><b>Method/field name remap</b>: rewrites SRG method/field names
 *       (func_N/field_N) in bytecode instructions and annotations to their
 *       notch names via {@code Remapper.mapMethodName/mapFieldName}.</li>
 *   <li><b>Annotation string remap</b>: rewrites the string values inside
 *       {@code @Mixin(targets=...)}, {@code @Inject(method=...)}, and
 *       {@code @Shadow(aliases=...)} annotations, since ASM's remapper does
 *       not touch annotation string values.</li>
 * </ol>
 *
 * <p>Usage: {@code MixinTargetPatcher <jarFile> <notch-srg.srg>}</p>
 */
public class MixinTargetPatcher {

    /** SRG internal class name -> notch internal class name. */
    static final Map<String, String> SRG_CLASS_TO_NOTCH = new HashMap<>();
    /** "owner/name desc" key -> notch method name. */
    static final Map<String, String> SRG_METHOD_TO_NOTCH = new HashMap<>();
    /** "owner/name desc" key -> notch field name. */
    static final Map<String, String> SRG_FIELD_TO_NOTCH = new HashMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: MixinTargetPatcher <jarFile> <notch-srg.srg>");
            System.exit(2);
            return;
        }
        File jarFile = new File(args[0]);
        File notchSrg = new File(args[1]);
        if (!jarFile.exists() || !notchSrg.exists()) {
            System.err.println("Missing input: jar=" + jarFile.exists() + " srg=" + notchSrg.exists());
            System.exit(2);
            return;
        }

        // Parse notch-srg.srg into three maps: classes, methods, fields.
        // Format:
        //   CL: <notch-class> <srg-class>
        //   MD: <notch-class>/<notch-method> <notch-desc> <srg-class>/<srg-method> <srg-desc>
        //   FD: <notch-class>/<notch-field> <srg-class>/<srg-field>
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new java.io.FileInputStream(notchSrg), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("CL: ")) {
                    String[] parts = line.split(" ");
                    if (parts.length >= 3) SRG_CLASS_TO_NOTCH.put(parts[2], parts[1]);
                } else if (line.startsWith("MD: ")) {
                    // MD: notch/notchMethod notchDesc srg/srgMethod srgDesc
                    // Note: the class part contains slashes too (net/minecraft/...),
                    // so the owner/method separator is the LAST slash.
                    String[] parts = line.substring(4).split(" ");
                    if (parts.length >= 4) {
                        String notchOwnerMethod = parts[0]; // notch-class/notch-method
                        String notchDesc = parts[1];
                        String srgOwnerMethod = parts[2];   // srg-class/srg-method
                        String srgDesc = parts[3];
                        int ns = notchOwnerMethod.lastIndexOf('/');
                        int ss = srgOwnerMethod.lastIndexOf('/');
                        if (ns > 0 && ss > 0) {
                            String srgOwner = srgOwnerMethod.substring(0, ss);
                            String srgName = srgOwnerMethod.substring(ss + 1);
                            String notchOwner = notchOwnerMethod.substring(0, ns);
                            String notchName = notchOwnerMethod.substring(ns + 1);
                            // Key by SRG owner+name+desc (canonical).
                            SRG_METHOD_TO_NOTCH.put(srgOwner + "." + srgName + srgDesc, notchName);
                            // Also key by notch owner+name+desc (the lookup uses
                            // the mixin's notch target as owner context).
                            SRG_METHOD_TO_NOTCH.put(notchOwner + "." + srgName + srgDesc, notchName);
                            // Also a name-only fallback (less precise).
                            SRG_METHOD_TO_NOTCH.putIfAbsent(srgName, notchName);
                        }
                    }
                } else if (line.startsWith("FD: ")) {
                    String[] parts = line.substring(4).split(" ");
                    if (parts.length >= 2) {
                        String notchOwnerField = parts[0];
                        String srgOwnerField = parts[1];
                        int ns = notchOwnerField.lastIndexOf('/');
                        int ss = srgOwnerField.lastIndexOf('/');
                        if (ns > 0 && ss > 0) {
                            String srgOwner = srgOwnerField.substring(0, ss);
                            String srgName = srgOwnerField.substring(ss + 1);
                            String notchName = notchOwnerField.substring(ns + 1);
                            SRG_FIELD_TO_NOTCH.put(srgOwner + "." + srgName, notchName);
                            SRG_FIELD_TO_NOTCH.putIfAbsent(srgName, notchName);
                        }
                    }
                }
            }
        }
        System.out.println("MixinTargetPatcher: loaded " + SRG_CLASS_TO_NOTCH.size()
            + " classes, " + SRG_METHOD_TO_NOTCH.size() + " methods, "
            + SRG_FIELD_TO_NOTCH.size() + " fields");

        // Remapper: rewrite SRG class names AND SRG method/field names to notch.
        Remapper remapper = new Remapper() {
            @Override
            public String map(String internal) {
                String notch = SRG_CLASS_TO_NOTCH.get(internal);
                return notch != null ? notch : internal;
            }
            @Override
            public String mapMethodName(String owner, String name, String desc) {
                String notch = SRG_METHOD_TO_NOTCH.get(owner + "." + name + desc);
                if (notch != null) return notch;
                notch = SRG_METHOD_TO_NOTCH.get(name);
                return notch != null && name.startsWith("func_") ? notch : name;
            }
            @Override
            public String mapInvokeDynamicMethodName(String name, String desc) {
                return name;
            }
            @Override
            public String mapFieldName(String owner, String name, String desc) {
                String notch = SRG_FIELD_TO_NOTCH.get(owner + "." + name);
                if (notch != null) return notch;
                notch = SRG_FIELD_TO_NOTCH.get(name);
                return notch != null && name.startsWith("field_") ? notch : name;
            }
        };

        File tmpOut = new File(jarFile.getParentFile(), jarFile.getName() + ".patched");
        int remapped = 0;
        int targetsRewritten = 0;
        try (JarFile jin = new JarFile(jarFile);
             JarOutputStream jout = new JarOutputStream(new FileOutputStream(tmpOut))) {
            java.util.Enumeration<JarEntry> entries = jin.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                byte[] bytes = readAll(jin.getInputStream(entry));
                if (entry.getName().endsWith(".class")) {
                    bytes = remapClass(bytes, remapper);
                    remapped++;
                    // For mixin classes, also convert @Mixin value to targets= String form.
                    if (entry.getName().contains("/mixin/")) {
                        byte[] rewritten = rewriteMixinTargets(bytes);
                        if (rewritten != null) {
                            bytes = rewritten;
                            targetsRewritten++;
                        }
                    }
                }
                jout.putNextEntry(new JarEntry(entry.getName()));
                jout.write(bytes);
                jout.closeEntry();
            }
        }
        // Atomic-ish swap.
        if (!tmpOut.renameTo(jarFile)) {
            java.nio.file.Files.copy(tmpOut.toPath(), jarFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            tmpOut.delete();
        }
        System.out.println("MixinTargetPatcher: remapped " + remapped + " classes, rewrote @Mixin targets in " + targetsRewritten);
    }

    /** Apply the SRG->notch remapper to a single class's bytecode. */
    private static byte[] remapClass(byte[] bytes, Remapper remapper) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(0);
        ClassRemapper cr = new ClassRemapper(writer, remapper);
        reader.accept(cr, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static byte[] rewriteMixinTargets(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.EXPAND_FRAMES);

        boolean changed = false;

        // Determine this mixin's notch target class (from @Mixin targets={...}).
        // We use it as the owner context when resolving overloaded SRG method names.
        String notchTargetClass = findMixinTargetClass(node);

        // 1. Class-level @Mixin: convert value=Type to targets=String (notch names).
        changed |= processAnnotations(collectAnnotationLists(node.visibleAnnotations, node.invisibleAnnotations), notchTargetClass);

        // 2. Method-level @Inject: rewrite method="func_..." to method="<notch>".
        if (node.methods != null) {
            for (Object mo : node.methods) {
                org.objectweb.asm.tree.MethodNode mn = (org.objectweb.asm.tree.MethodNode) mo;
                changed |= processAnnotations(collectAnnotationLists(
                    mn.visibleAnnotations, mn.invisibleAnnotations), notchTargetClass);
            }
        }

        // 3. Field-level @Shadow: rewrite aliases={"field_..."} to notch names.
        if (node.fields != null) {
            for (Object fo : node.fields) {
                org.objectweb.asm.tree.FieldNode fn = (org.objectweb.asm.tree.FieldNode) fo;
                changed |= processAnnotations(collectAnnotationLists(
                    fn.visibleAnnotations, fn.invisibleAnnotations), notchTargetClass);
            }
        }

        if (!changed) return null;
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    /** Extract the notch target class name from the class's @Mixin(targets=...)
     *  or @Mixin(value=Type.class) annotation. */
    @SuppressWarnings("unchecked")
    private static String findMixinTargetClass(ClassNode node) {
        List<List<?>> lists = collectAnnotationLists(node.visibleAnnotations, node.invisibleAnnotations);
        for (List<?> annList : lists) {
            for (Object ao : annList) {
                AnnotationNode ann = (AnnotationNode) ao;
                if (!"Lorg/spongepowered/asm/mixin/Mixin;".equals(ann.desc)) continue;
                if (ann.values == null) continue;
                for (int i = 0; i + 1 < ann.values.size(); i += 2) {
                    String key = (String) ann.values.get(i);
                    Object v = ann.values.get(i + 1);
                    if ("targets".equals(key)) {
                        // String-form targets: List<String> of notch names.
                        if (v instanceof List && !((List<?>) v).isEmpty()) {
                            return (String) ((List<?>) v).get(0);
                        }
                    } else if ("value".equals(key)) {
                        // Type-form value: List<Type> (after ClassRemapper, the
                        // types are notch names like "Lbee;"). Extract the class.
                        if (v instanceof List) {
                            for (Object t : (List<?>) v) {
                                String cls = typeToInternal(t);
                                if (!cls.isEmpty()) return cls;
                            }
                        } else {
                            String cls = typeToInternal(v);
                            if (!cls.isEmpty()) return cls;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static List<List<?>> collectAnnotationLists(List<?> a, List<?> b) {
        List<List<?>> out = new ArrayList<>();
        if (a != null) out.add(a);
        if (b != null) out.add(b);
        return out;
    }

    /**
     * Walk each annotation list and rewrite string values that reference SRG
     * method/field names to notch names. Also converts @Mixin value=Type to
     * targets=String array.
     *
     * @return true if any annotation was modified
     */
    private static boolean processAnnotations(List<List<?>> annotationLists, String ownerCtx) {
        boolean changed = false;
        for (List<?> annList : annotationLists) {
            for (Object ao : annList) {
                AnnotationNode ann = (AnnotationNode) ao;
                if (ann.values == null) continue;

                if ("Lorg/spongepowered/asm/mixin/Mixin;".equals(ann.desc)) {
                    // Convert value=Type (or List<Type>) to targets=List<String>.
                    List<String> targetNotch = new ArrayList<>();
                    boolean hasValue = false;
                    for (int i = 0; i + 1 < ann.values.size(); i += 2) {
                        if ("value".equals(ann.values.get(i))) {
                            hasValue = true;
                            Object v = ann.values.get(i + 1);
                            if (v instanceof List) {
                                for (Object t : (List<?>) v) targetNotch.add(typeToInternal(t));
                            } else {
                                targetNotch.add(typeToInternal(v));
                            }
                        }
                    }
                    if (hasValue && !targetNotch.isEmpty()) {
                        List<Object> newValues = new ArrayList<>();
                        for (int i = 0; i + 1 < ann.values.size(); i += 2) {
                            if ("value".equals(ann.values.get(i))) continue;
                            newValues.add(ann.values.get(i));
                            newValues.add(ann.values.get(i + 1));
                        }
                        newValues.add("targets");
                        newValues.add(new ArrayList<>(targetNotch));
                        ann.values = newValues;
                        changed = true;
                    }
                } else if ("Lorg/spongepowered/asm/mixin/injection/Inject;".equals(ann.desc)
                        || "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;".equals(ann.desc)
                        || "Lorg/spongepowered/asm/mixin/injection/Redirect;".equals(ann.desc)
                        || "Lorg/spongepowered/asm/mixin/injection/ModifyArg;".equals(ann.desc)
                        || "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;".equals(ann.desc)
                        || "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;".equals(ann.desc)
                        || "Lorg/spongepowered/asm/mixin/injection/ModifyReceiver;".equals(ann.desc)
                        || "Lorg/spongepowered/asm/mixin/injection/ModifyReturnValue;".equals(ann.desc)) {
                    // Rewrite method = "func_xxx(...)" or method = {"func_xxx", ...}.
                    changed |= rewriteStringOrList(ann, "method", ownerCtx);
                } else if ("Lorg/spongepowered/asm/mixin/Shadow;".equals(ann.desc)) {
                    // Rewrite aliases = {"field_xxx", ...}.
                    changed |= rewriteStringOrList(ann, "aliases", ownerCtx);
                } else if ("Lorg/spongepowered/asm/mixin/injection/At;".equals(ann.desc)) {
                    // Rewrite target = "Lnet/.../X;func_yyy(...)V" descriptors.
                    changed |= rewriteStringOrList(ann, "target", ownerCtx);
                }
            }
        }
        return changed;
    }

    /**
     * Rewrite a string or string-array annotation value (named by key) in place,
     * replacing any SRG method/field name token AND any SRG class references in
     * descriptors with their notch names. ownerCtx is the notch target class of
     * the owning mixin, used to resolve overloaded methods precisely. Returns
     * true if anything changed.
     */
    private static boolean rewriteStringOrList(AnnotationNode ann, String key, String ownerCtx) {
        boolean changed = false;
        for (int i = 0; i + 1 < ann.values.size(); i += 2) {
            if (!key.equals(ann.values.get(i))) continue;
            Object v = ann.values.get(i + 1);
            System.out.println("    [rewriteStringOrList] key=" + key + " ownerCtx=" + ownerCtx + " value=" + v + " class=" + (v == null ? "null" : v.getClass().getName()));
            if (v instanceof String) {
                String rewritten = rewriteMemberString((String) v, ownerCtx);
                if (!rewritten.equals(v)) {
                    ann.values.set(i + 1, rewritten);
                    changed = true;
                    System.out.println("      -> rewritten to " + rewritten);
                } else {
                    System.out.println("      -> no change");
                }
            } else if (v instanceof List) {
                List<Object> newList = new ArrayList<>();
                boolean listChanged = false;
                for (Object s : (List<?>) v) {
                    if (s instanceof String) {
                        String rewritten = rewriteMemberString((String) s, ownerCtx);
                        System.out.println("        list elem: '" + s + "' -> '" + rewritten + "' (owner=" + ownerCtx + ")");
                        if (!rewritten.equals(s)) listChanged = true;
                        newList.add(rewritten);
                    } else {
                        newList.add(s);
                    }
                }
                if (listChanged) {
                    ann.values.set(i + 1, newList);
                    changed = true;
                    System.out.println("      -> list rewritten");
                }
            }
        }
        return changed;
    }

    /**
     * Rewrite SRG method/field name tokens and class references inside an
     * annotation string value. Handles strings like "func_71407_l()V",
     * "func_78480_b(F)V", or descriptors "Lnet/.../X;func_yyy(...)V". Method
     * names are resolved using owner context for precision when overloaded.
     */
    private static String rewriteMemberString(String s, String ownerCtx) {
        if (s == null || s.isEmpty()) return s;

        // 1. Rewrite SRG class references Lnet/minecraft/.../X; to notch Lxxx;.
        java.util.regex.Matcher mc = java.util.regex.Pattern.compile("L([^;]+);").matcher(s);
        StringBuffer sbc = new StringBuffer();
        boolean classChanged = false;
        while (mc.find()) {
            String cls = mc.group(1);
            String notch = SRG_CLASS_TO_NOTCH.get(cls);
            if (notch != null) {
                mc.appendReplacement(sbc, "L" + java.util.regex.Matcher.quoteReplacement(notch) + ";");
                classChanged = true;
            }
        }
        mc.appendTail(sbc);
        String result = classChanged ? sbc.toString() : s;

        // 2. Rewrite SRG method name tokens func_xxxxxx_x[_] to notch names,
        //    preferring owner-qualified lookups. SRG method names may end with
        //    a trailing underscore (e.g. func_73866_w_), so include it.
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("func_[0-9]+_[a-zA-Z_]+").matcher(result);
        StringBuffer sb = new StringBuffer();
        boolean methodChanged = false;
        while (m.find()) {
            String token = m.group();
            String notch = null;
            // Extract descriptor tail from the match position for owner lookup.
            String tail = result.substring(m.end());
            // Try owner.name+desc first.
            // Find the descriptor: "(<params>)<returntype>". We need the full
            // descriptor including the return type for an exact key match.
            int paren = tail.indexOf('(');
            if (paren >= 0 && ownerCtx != null) {
                int close = tail.indexOf(')');
                if (close > paren) {
                    // Return type: the token after ')'. For simplicity, take up
                    // to the next non-descript char or end. Descriptors look like
                    // "()V", "(FZII)V", "(Lnet/.../X;F)Z". The return type is a
                    // single primitive letter OR "L<class>;" OR "[<type>".
                    String returnType = "";
                    int rt = close + 1;
                    if (rt < tail.length()) {
                        char c = tail.charAt(rt);
                        if (c == 'L') {
                            int sc = tail.indexOf(';', rt);
                            if (sc > rt) returnType = tail.substring(rt, sc + 1);
                        } else if (c == '[') {
                            // array — take until primitive/class end
                            int end = rt + 1;
                            while (end < tail.length() && tail.charAt(end) == '[') end++;
                            if (end < tail.length() && tail.charAt(end) == 'L') {
                                int sc = tail.indexOf(';', end);
                                if (sc > end) returnType = tail.substring(rt, sc + 1);
                            } else if (end < tail.length()) {
                                returnType = tail.substring(rt, end + 1);
                            }
                        } else {
                            returnType = tail.substring(rt, rt + 1);
                        }
                    }
                    String desc = tail.substring(paren, close + 1) + returnType;
                    notch = SRG_METHOD_TO_NOTCH.get(ownerCtx + "." + token + desc);
                }
            }
            if (notch == null) notch = SRG_METHOD_TO_NOTCH.get(token);
            if (notch != null) {
                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(notch));
                methodChanged = true;
            }
        }
        m.appendTail(sb);
        if (methodChanged) result = sb.toString();

        // 3. Rewrite SRG field name tokens field_xxxxxx_x to notch names.
        java.util.regex.Matcher mf = java.util.regex.Pattern.compile("field_[0-9]+_[a-zA-Z]+").matcher(result);
        StringBuffer sbf = new StringBuffer();
        boolean fieldChanged = false;
        while (mf.find()) {
            String notch = SRG_FIELD_TO_NOTCH.get(mf.group());
            if (notch != null) {
                mf.appendReplacement(sbf, java.util.regex.Matcher.quoteReplacement(notch));
                fieldChanged = true;
            }
        }
        mf.appendTail(sbf);
        if (fieldChanged) result = sbf.toString();

        return (classChanged || methodChanged || fieldChanged) ? result : s;
    }

    /** Convert an org.objectweb.asm.Type to internal class name (slashes). */
    private static String typeToInternal(Object typeObj) {
        if (typeObj == null) return "";
        String s = typeObj.toString();
        if (s.startsWith("L") && s.endsWith(";")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
