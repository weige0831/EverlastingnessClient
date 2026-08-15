package net.everlastingness.client.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;

import net.everlastingness.client.mixinhost.MixinClassFileTransformer;

/**
 * Java Agent premain for the modern-era (1.20.x) Everlastingness client.
 *
 * <p>The launcher starts vanilla 1.20.x with
 * {@code -javaagent:everlastingness-1.20.1.jar}; this {@code premain} runs
 * before {@code net.minecraft.client.main.Main} and bootstraps the standalone
 * Mixin host so the reobfuscated mixin jar's hooks (e.g. {@code @Mixin(fjq)}
 * on the obfuscated {@code GameRenderer}) apply to the vanilla classes.</p>
 *
 * <p>Bootstrap sequence (the standalone Mixin host, no LaunchWrapper):</p>
 * <ol>
 *   <li>{@link MixinBootstrap#init()} triggers service discovery via
 *       {@code ServiceLoader}, selecting our {@code StandaloneMixinService}
 *       (its {@code isValid()} returns true) and offering the transformer
 *       factory to it.</li>
 *   <li>Retrieve the {@link IMixinTransformerFactory} internal from the service
 *       and build an {@link IMixinTransformer}.</li>
 *   <li>Load the mixin config ({@code everlastingness.mixins.json}) so the
 *       transformer knows which mixins to apply.</li>
 *   <li>Register a {@link MixinClassFileTransformer} wrapping the transformer
 *       with the JVM {@link Instrumentation}, so every class load is routed
 *       through Mixin.</li>
 * </ol>
 */
public class EverlastingnessAgent {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/Agent");

    public static void premain(String args, Instrumentation instrumentation) {
        String mcVersion = System.getProperty("everlastingness.version", "");
        // Mixin reads debug options from system properties when each environment
        // is constructed; set them BEFORE any Mixin class initializes. The
        // obfuscation type makes the refmap remapper active (with no context the
        // refmap silently maps nothing).
        System.setProperty("mixin.debug.verbose", "true");
        System.setProperty("mixin.debug.count", "true");
        System.setProperty("mixin.env.obfuscationType", "intermediary");
        LOGGER.info("Everlastingness agent attached (premain) for Minecraft " + mcVersion);

        if (instrumentation == null) {
            LOGGER.severe("No Instrumentation available — cannot apply mixins. Aborting.");
            return;
        }

        try {
            bootstrapMixin(instrumentation, mcVersion);
        } catch (Throwable t) {
            LOGGER.severe("Failed to bootstrap Mixin host: " + t);
            t.printStackTrace();
        }
    }

    /** Agentmain entry for dynamic attach (same body as premain). */
    public static void agentmain(String args, Instrumentation instrumentation) {
        premain(args, instrumentation);
    }

    /**
     * Extracts each nested jar under {@code /META-INF/jars/} to a temp dir and
     * appends it to the system class loader search via
     * {@link Instrumentation#appendToSystemClassLoaderSearch}. Jars already
     * covering a class (e.g. the -cp mixin jar) are still appended — harmless
     * duplication — but the ASM jars are essential: Mixin and our bytecode
     * provider need {@code org.objectweb.asm} at runtime and it is only bundled
     * here.
     */
    private static void appendNestedJars(Instrumentation instrumentation) {
        ClassLoader self = EverlastingnessAgent.class.getClassLoader();
        java.io.File dir = new java.io.File(System.getProperty("java.io.tmpdir"), "everlastingness-jars");
        dir.mkdirs();
        String[] names = { "mixin-0.8.7.jar", "asm-9.6.jar", "asm-commons-9.6.jar", "asm-tree-9.6.jar", "asm-util-9.6.jar", "asm-analysis-9.6.jar" };
        for (String name : names) {
            try {
                java.net.URL url = self.getResource("META-INF/jars/" + name);
                if (url == null) {
                    continue;
                }
                java.io.File out = new java.io.File(dir, name);
                try (java.io.InputStream in = url.openStream();
                     java.io.OutputStream os = new java.io.FileOutputStream(out)) {
                    in.transferTo(os);
                }
                instrumentation.appendToSystemClassLoaderSearch(new java.util.jar.JarFile(out));
                LOGGER.info("Appended nested jar to system classpath: " + name);
            } catch (Throwable t) {
                LOGGER.warning("Could not append nested jar " + name + ": " + t);
            }
        }
    }

    private static void bootstrapMixin(Instrumentation instrumentation, String mcVersion) throws Exception {
        // 0. Extract the nested runtime jars (mixin + asm) to disk and append them
        //    to the SYSTEM class loader search. They live under META-INF/jars in
        //    this agent jar, which the classpath cannot see; appending them makes
        //    Mixin, ASM and our StandaloneMixinService provider all loadable from
        //    the SAME app classloader (a child URLClassLoader would give Mixin a
        //    second, distinct IMixinService interface and every provider would
        //    then fail the instanceof check as an "invalid service").
        appendNestedJars(instrumentation);

        // 1. Bootstrap Mixin: discover our StandaloneMixinService via ServiceLoader,
        //    prepare the environment, and offer the transformer factory to it.
        LOGGER.info("Bootstrapping Mixin host...");
        ClassLoader cl = EverlastingnessAgent.class.getClassLoader();
        Class<?> bootstrap = Class.forName("org.spongepowered.asm.launch.MixinBootstrap", true, cl);
        Method init = bootstrap.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(null);

        // 2. Obtain the transformer via the IMixinTransformerFactory internal that
        //    MixinBootstrap offered to the service during init().
        IMixinService service = MixinService.getService();
        IMixinTransformerFactory factory = getFactory(service);
        if (factory == null) {
            LOGGER.severe("Mixin service did not offer an IMixinTransformerFactory. " +
                    "Mixins will not apply.");
            return;
        }
        IMixinTransformer transformer = factory.createTransformer();
        LOGGER.info("Mixin transformer acquired.");

        // 3. In a standalone agent host nothing sets the Mixin phase (no tweaker,
        //    no mod launcher); the bootstrap services leave it at PREINIT.
        org.spongepowered.asm.mixin.MixinEnvironment env =
                org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment();

        // 4. Mixins.addConfiguration registers the config against the DEFAULT
        //    environment. MixinConfig.select() rejects configs whose env differs
        //    from the runtime environment — with the runtime stuck at PREINIT no
        //    mixin would ever apply. Force the phase to DEFAULT FIRST so both
        //    sides agree (currentPhase/currentEnvironment are package-private
        //    statics; gotoPhase is package-private too, hence reflection).
        try {
            java.lang.reflect.Field currentPhase = org.spongepowered.asm.mixin.MixinEnvironment.class
                    .getDeclaredField("currentPhase");
            currentPhase.setAccessible(true);
            Object phase = currentPhase.get(null);
            currentPhase.set(null, org.spongepowered.asm.mixin.MixinEnvironment.Phase.DEFAULT);
            java.lang.reflect.Field currentEnv = org.spongepowered.asm.mixin.MixinEnvironment.class
                    .getDeclaredField("currentEnvironment");
            currentEnv.setAccessible(true);
            currentEnv.set(null, null);
            // Re-resolve the environment handle AFTER the phase switch.
            env = org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment();
            LOGGER.info("Forced Mixin phase DEFAULT (was " + phase + "), env=" + env.getPhase());
        } catch (Throwable t) {
            LOGGER.warning("Could not force DEFAULT phase: " + t);
        }
        try {
            String mappingRes = "mappings/inter2obf-" + mcVersion + ".json";
            ClassLoader self = EverlastingnessAgent.class.getClassLoader();
            if (mcVersion.isEmpty() || self.getResource(mappingRes) == null) {
                // Fall back to the single bundled mapping table.
                mappingRes = "mappings/inter2obf-1.20.1.json";
            }
            env.getRemappers().add(new net.everlastingness.client.mixinhost.IntermediaryToObfRemapper(mappingRes));
            LOGGER.info("Registered intermediary->obf remapper (" + mappingRes + ").");
        } catch (Throwable t) {
            LOGGER.warning("intermediary->obf remapper unavailable: " + t
                    + " — mixins will only apply if targets are already runtime names.");
        }
        // Verbose mode emits "Mixing <mixin> from <config> into <target>" per
        // applied mixin — the launcher's E2E harness greps for these lines.
        env.setOption(org.spongepowered.asm.mixin.MixinEnvironment.Option.DEBUG_VERBOSE, true);
        env.setOption(org.spongepowered.asm.mixin.MixinEnvironment.Option.DEBUG_ALL, true);
        env.setOption(org.spongepowered.asm.mixin.MixinEnvironment.Option.DUMP_TARGET_ON_FAILURE, true);
        String configs = System.getProperty("mixin.configs", "everlastingness.mixins.json");
        for (String config : configs.split(",")) {
            String trimmed = config.trim();
            if (!trimmed.isEmpty()) {
                // Two-arg variant supplies a non-null default environment (the
                // one-arg form passes null and NPEs in MixinConfig.onLoad in a
                // standalone host) and registers into Mixins.getConfigs(), the
                // registry MixinProcessor.selectConfigs() iterates.
                Mixins.addConfiguration(trimmed, null);
                LOGGER.info("Loaded mixin config: " + trimmed);
            }
        }

        // 5. Register the transformer so every class load is routed through Mixin.
        instrumentation.addTransformer(new MixinClassFileTransformer(transformer), true);
        // Prime Mixin's select pass NOW: the JVM can invoke the transformer for
        // class loads that happen while premain is still running (before the
        // config was registered), and checkSelect() will not re-select once
        // transformedCount > 0 unless the environment changes. Transforming a
        // synthetic non-target class forces select() and populates pendingConfigs.
        try {
            org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
            cw.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                    "net/everlastingness/client/PrimeProbe", null, "java/lang/Object", null);
            cw.visitEnd();
            byte[] probeBytes = cw.toByteArray();
            transformer.transformClassBytes("net.everlastingness.client.PrimeProbe",
                    "net.everlastingness.client.PrimeProbe", probeBytes);
            // Report the select()/prepare() outcome for the environment config.
            java.lang.reflect.Field mixinsField = Class.forName(
                    "org.spongepowered.asm.mixin.transformer.MixinConfig").getDeclaredField("mixins");
            mixinsField.setAccessible(true);
            for (org.spongepowered.asm.mixin.transformer.Config cfg : Mixins.getConfigs()) {
                LOGGER.info("Config " + cfg.getName() + " visited=" + cfg.isVisited());
            }
            LOGGER.info("Post-prime: env-configs=" + env.getMixinConfigs().size()
                    + " registry=" + Mixins.getConfigs().size());
            for (String cfgName : env.getMixinConfigs()) {
                LOGGER.info("Env config listed: " + cfgName);
            }
            LOGGER.info("Prime transform done.");
        } catch (Throwable t) {
            LOGGER.warning("Prime transform failed (benign if no mixins selected): " + t);
        }
        // Mark this transformer active on the environment (MixinEnvironment uses
        // the active transformer to decide whether mixin application is enabled)
        // and log config visibility diagnostics.
        try {
            org.spongepowered.asm.mixin.MixinEnvironment cur =
                    org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment();
            cur.setActiveTransformer(transformer);
            LOGGER.info("Everlastingness client active on " + mcVersion
                    + ". Environment=" + cur.getPhase()
                    + " env-configs=" + cur.getMixinConfigs()
                    + " mixins-registry=" + Mixins.getConfigs().size()
                    + " unvisited=" + Mixins.getUnvisitedCount());
        } catch (Throwable t) {
            LOGGER.info("Everlastingness client active on " + mcVersion + " (env diagnostics unavailable: " + t + ")");
        }
        // Offline probe support: with -Deverlastingness.probe=<internalName> the
        // agent transforms that class from the vanilla jar and reports whether
        // any mixin applied — used by the standalone ApplyProbe tool.
        String probe = System.getProperty("everlastingness.probe");
        if (probe != null && !probe.isEmpty()) {
            probeApply(transformer, probe);
        }
    }

    private static void probeApply(IMixinTransformer transformer, String internalName) {
        try {
            String jarPath = System.getProperty("everlastingness.vanillajar", "");
            if (jarPath.isEmpty()) {
                LOGGER.severe("probe: set -Deverlastingness.vanillajar=<path>");
                return;
            }
            byte[] bytes;
            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath)) {
                java.util.jar.JarEntry e = jar.getJarEntry(internalName + ".class");
                if (e == null) {
                    LOGGER.severe("probe: class not found in vanilla jar: " + internalName);
                    return;
                }
                try (java.io.InputStream in = jar.getInputStream(e);
                     java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                    in.transferTo(out);
                    bytes = out.toByteArray();
                }
            }
            byte[] result = transformer.transformClassBytes(internalName, internalName, bytes);
            boolean changed = result != null && !java.util.Arrays.equals(result, bytes);
            LOGGER.info("probe: " + internalName + " transformed=" + changed
                    + " (in " + bytes.length + "B, out "
                    + (result == null ? "null" : result.length + "B"));
            if (changed) {
                LOGGER.info("probe: MIXIN APPLIED ✓");
            } else {
                LOGGER.warning("probe: no mixin applied for " + internalName);
            }
        } catch (Throwable t) {
            LOGGER.severe("probe failed: " + t);
            t.printStackTrace();
        }
    }

    /**
     * Retrieve the {@link IMixinTransformerFactory} that {@code MixinBootstrap}
     * offered to the service. {@code getInternal(Class)} is protected on
     * {@code MixinServiceAbstract}; reach it via reflection.
     */
    private static IMixinTransformerFactory getFactory(IMixinService service) throws Exception {
        if (service instanceof IMixinTransformerFactory) {
            return (IMixinTransformerFactory) service;
        }
        // getInternal(Class) is a protected instance method on MixinServiceAbstract
        // (not public), so getDeclaredMethod on the concrete class + setAccessible.
        Method getInternal = service.getClass().getDeclaredMethod("getInternal", Class.class);
        getInternal.setAccessible(true);
        try {
            return (IMixinTransformerFactory) getInternal.invoke(service, IMixinTransformerFactory.class);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() != null) {
                throw (Exception) (e.getCause() instanceof Exception ? e.getCause() : e);
            }
            throw e;
        }
    }
}
