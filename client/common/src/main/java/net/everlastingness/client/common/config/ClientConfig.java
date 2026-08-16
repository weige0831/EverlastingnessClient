package net.everlastingness.client.common.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client configuration with JSON persistence in
 * {@code ~/.everlastingness/client/modules.json} — the same file the launcher
 * UI reads and writes, so module toggles made before launch apply in-game and
 * survive restarts.
 *
 * <p>Format is a flat {@code {"moduleId": true|false}} object; unknown ids are
 * ignored by the game and preserved on save. Parsing is a minimal hand-rolled
 * scanner (no Gson dependency in :common).</p>
 */
public final class ClientConfig {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/Config");

    private final Map<String, Boolean> moduleEnabled = new LinkedHashMap<>();
    private final Path file = configPath();

    public ClientConfig() {
        load();
    }

    /** Resolve ~/.everlastingness/client/modules.json (overridable for tests). */
    private static Path configPath() {
        String override = System.getProperty("everlastingness.modules.config");
        if (override != null && !override.isEmpty()) {
            return Paths.get(override);
        }
        String home = System.getProperty("user.home", ".");
        return Paths.get(home, ".everlastingness", "client", "modules.json");
    }

    /** Whether a module should be enabled, falling back to its default. */
    public boolean getModuleEnabled(String moduleId, boolean defaultEnabled) {
        return moduleEnabled.getOrDefault(moduleId, defaultEnabled);
    }

    /** Record the user's enable/disable choice for a module and persist it. */
    public void setModuleEnabled(String moduleId, boolean enabled) {
        moduleEnabled.put(moduleId, enabled);
        save();
    }

    /** Immutable snapshot of module enable flags. */
    public Map<String, Boolean> moduleFlags() {
        return new LinkedHashMap<>(moduleEnabled);
    }

    // --- persistence ---------------------------------------------------------

    private void load() {
        if (!Files.isReadable(file)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            moduleEnabled.putAll(parse(r));
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Could not read " + file + ": " + t);
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                w.write(write(moduleEnabled));
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not save " + file + ": " + e);
        }
    }

    /** Minimal flat-object JSON parser: {"id":bool,...} with string escapes. */
    static Map<String, Boolean> parse(Reader reader) throws IOException {
        Map<String, Boolean> out = new LinkedHashMap<>();
        StringBuilder token = new StringBuilder();
        int c;
        char expect = '{';
        String key = null;
        while ((c = reader.read()) >= 0) {
            char ch = (char) c;
            if (Character.isWhitespace(ch)) {
                continue;
            }
            if (expect == '{' || expect == ',' || expect == '}') {
                if ((expect == '{' && ch == '{') || (expect == ',' && ch == ',')) {
                    expect = '"';
                    continue;
                }
                // Empty object: "{" immediately followed by "}".
                if (expect == '{' && ch == '}') {
                    break;
                }
                if (expect == '}' && ch == '}') {
                    break;
                }
                // Trailing separator before close: {"a":true,} — tolerate.
                if ((expect == '"' || expect == ',') && ch == '}') {
                    break;
                }
                throw new IOException("Unexpected '" + ch + "' (expected " + expect + ")");
            }
            if (expect == '"') {
                // Empty object close after "{", or trailing separator.
                if (ch == '}') {
                    break;
                }
                if (ch != '"') {
                    throw new IOException("Expected key start quote, got '" + ch + "'");
                }
                token.setLength(0);
                while ((c = reader.read()) >= 0) {
                    ch = (char) c;
                    if (ch == '\\') {
                        int next = reader.read();
                        token.append(next == 'n' ? '\n' : next == 't' ? '\t' : (char) next);
                    } else if (ch == '"') {
                        break;
                    } else {
                        token.append(ch);
                    }
                }
                key = token.toString();
                expect = ':';
                continue;
            }
            if (expect == ':') {
                if (ch != ':') {
                    throw new IOException("Expected ':', got '" + ch + "'");
                }
                token.setLength(0);
                while ((c = reader.read()) >= 0) {
                    ch = (char) c;
                    if (ch == '}' || ch == ',') {
                        break;
                    }
                    token.append(ch);
                }
                String value = token.toString().trim();
                out.put(key, "true".equals(value));
                if (ch == '}') {
                    break;
                }
                // ch == ',': the separator is consumed here; expect the next key.
                expect = '"';
                continue;
            }
        }
        return out;
    }

    /** Serialise a flat id→enabled map as compact JSON. */
    static String write(Map<String, Boolean> flags) {
        StringBuilder sb = new StringBuilder("{\n");
        boolean first = true;
        for (Map.Entry<String, Boolean> e : flags.entrySet()) {
            if (!first) {
                sb.append(",\n");
            }
            first = false;
            sb.append("  \"").append(e.getKey().replace("\\", "\\\\").replace("\"", "\\\""))
              .append("\": ").append(e.getValue());
        }
        sb.append("\n}\n");
        return sb.toString();
    }
}
