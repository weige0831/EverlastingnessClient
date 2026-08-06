package net.everlastingness.client.common.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory client configuration. Module enable/disable flags are stored here;
 * a future persistence layer (JSON in {@code ~/.everlastingness/client/config.json})
 * will load/save on top of this.
 *
 * <p>Kept deliberately simple and dependency-free for the Phase 0 skeleton;
 * the launcher reads/writes the same file so user changes survive restarts.</p>
 */
public final class ClientConfig {

    private final Map<String, Boolean> moduleEnabled = new LinkedHashMap<>();

    /** Whether a module should be enabled, falling back to its default. */
    public boolean getModuleEnabled(String moduleId, boolean defaultEnabled) {
        return moduleEnabled.getOrDefault(moduleId, defaultEnabled);
    }

    /** Record the user's enable/disable choice for a module. */
    public void setModuleEnabled(String moduleId, boolean enabled) {
        moduleEnabled.put(moduleId, enabled);
    }

    /** Immutable snapshot of module enable flags. */
    public Map<String, Boolean> moduleFlags() {
        return new LinkedHashMap<>(moduleEnabled);
    }
}
