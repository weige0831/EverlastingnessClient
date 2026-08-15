package net.everlastingness.client.mixinhost;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.extensibility.IRemapper;

/**
 * Maps intermediary names ({@code net/minecraft/class_310}) to the official
 * obfuscated runtime names ({@code enn}) so mixins compiled against Yarn /
 * intermediary apply to a vanilla (obfuscated) Minecraft process.
 *
 * <p>The table is generated at build time from Fabric's
 * {@code net.fabricmc:intermediary} tiny mappings (whose {@code official}
 * column is the obfuscated name) and shipped as
 * {@code mappings/inter2obf-<version>.json} inside the agent jar.</p>
 */
public final class IntermediaryToObfRemapper implements IRemapper {

    private final Map<String, String> classes = new HashMap<>();
    /** intermediary member name -> obf simple name. */
    private final Map<String, String> members = new HashMap<>();

    public IntermediaryToObfRemapper(String resource) throws Exception {
        ClassLoader cl = IntermediaryToObfRemapper.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Mapping resource not found: " + resource);
            }
            com.google.gson.Gson gson = new com.google.gson.Gson();
            Table table = gson.fromJson(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8), Table.class);
            if (table.classes != null) {
                classes.putAll(table.classes);
            }
            // members: intermediary -> [obfOwner, obfName]; keep only the name
            if (table.members != null) {
                for (Map.Entry<String, String[]> e : table.members.entrySet()) {
                    members.put(e.getKey(), e.getValue()[1]);
                }
            }
        }
    }

    @Override
    public String map(String internalName) {
        if (internalName == null) {
            return null;
        }
        String mapped = classes.get(internalName);
        if (mapped == null && internalName.startsWith("net/minecraft/class_")) {
            System.out.println("[Everlastingness-remapper] UNMAPPED class target: " + internalName);
        }
        return mapped != null ? mapped : internalName;
    }

    @Override
    public String unmap(String internalName) {
        for (Map.Entry<String, String> e : classes.entrySet()) {
            if (e.getValue().equals(internalName)) {
                return e.getKey();
            }
        }
        return internalName;
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        String mapped = members.get(name);
        return mapped != null ? mapped : name;
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        String mapped = members.get(name);
        return mapped != null ? mapped : name;
    }

    @Override
    public String mapDesc(String desc) {
        if (desc == null || !desc.startsWith("L")) {
            return desc;
        }
        // Field/method descriptor: remap L-internal; types.
        StringBuilder sb = new StringBuilder(desc.length());
        int i = 0;
        while (i < desc.length()) {
            char c = desc.charAt(i);
            if (c == 'L') {
                int end = desc.indexOf(';', i);
                if (end < 0) {
                    return desc;
                }
                sb.append('L').append(map(desc.substring(i + 1, end))).append(';');
                i = end + 1;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    @Override
    public String unmapDesc(String desc) {
        return desc; // Not needed for applying mixins.
    }

    private static final class Table {
        Map<String, String> classes;
        Map<String, String[]> members;
    }
}
