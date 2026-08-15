package net.everlastingness.client.v1_7_10.tweaker;

import org.spongepowered.asm.mixin.extensibility.IRemapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * A Mixin {@link IRemapper} that maps SRG (deobf) class names to their notch
 * equivalents for the production MC 1.7.10 jar.
 *
 * <p>MC 1.7.10 ships with notch-obfuscated class names (e.g. {@code blt} for
 * {@code EntityRenderer}) but SRG method names ({@code func_*}). The
 * {@code @Mixin(X.class)} target annotations are patched to notch string form
 * at build time (see {@code MixinTargetPatcher}), so target class resolution
 * already works. This remapper covers the remaining cases: any SRG class names
 * Mixin resolves through descriptor parsing (e.g. method descriptors in
 * {@code @Inject} {@code at} selectors) are remapped to their notch names.</p>
 *
 * <p>The SRG→notch table is loaded from {@code srg-to-notch.tsv} (generated at
 * build time from Forge's {@code notch-srg.srg}). Method and field names are
 * left untouched because the refmap already emits SRG names matching the
 * runtime SRG method/field names.</p>
 */
public final class NotchRemapper implements IRemapper {

    /** SRG internal name (slashes) -> notch internal name. */
    private final Map<String, String> srgToNotch = new HashMap<>();
    /** Notch internal name -> SRG internal name (inverse, for unmap). */
    private final Map<String, String> notchToSrg = new HashMap<>();

    public NotchRemapper() {
        load("/srg-to-notch.tsv");
    }

    private void load(String resource) {
        int count = 0;
        try (InputStream in = NotchRemapper.class.getResourceAsStream(resource)) {
            if (in == null) {
                System.err.println("[Everlastingness] NotchRemapper: " + resource + " not found on classpath");
                return;
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    int tab = line.indexOf('\t');
                    if (tab > 0 && tab < line.length() - 1) {
                        String srg = line.substring(0, tab);
                        String notch = line.substring(tab + 1);
                        srgToNotch.put(srg, notch);
                        notchToSrg.put(notch, srg);
                        count++;
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[Everlastingness] NotchRemapper: failed to load " + resource + ": " + t);
        }
        System.out.println("[Everlastingness] NotchRemapper loaded " + count + " srg\u2192notch mappings");
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        return null;
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        return null;
    }

    @Override
    public String map(String internalName) {
        return srgToNotch.get(internalName);
    }

    @Override
    public String unmap(String internalName) {
        return notchToSrg.get(internalName);
    }

    @Override
    public String mapDesc(String desc) {
        return null;
    }

    @Override
    public String unmapDesc(String desc) {
        return null;
    }
}
