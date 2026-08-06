package net.everlastingness.client.common.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class providing enable/disable state machine and metadata storage for
 * {@link Module} implementations. Subclasses implement {@link #onEnable()}
 * and {@link #onDisable()}.
 */
public abstract class AbstractModule implements Module {

    private volatile boolean enabled;

    /** Per-module key/value settings (persisted by the config system). */
    private final Map<String, Object> settings = new LinkedHashMap<>();

    @Override
    public final synchronized void enable() {
        if (enabled) {
            return;
        }
        enabled = true;
        onEnable();
    }

    @Override
    public final synchronized void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        onDisable();
    }

    @Override
    public final boolean isEnabled() {
        return enabled;
    }

    /** Read a module setting, returning the default if unset. */
    @SuppressWarnings("unchecked")
    public <T> T getSetting(String key, T defaultValue) {
        Object v = settings.get(key);
        return v == null ? defaultValue : (T) v;
    }

    /** Write a module setting. */
    public void setSetting(String key, Object value) {
        settings.put(key, value);
    }

    /** An immutable view of the module's current settings. */
    public Map<String, Object> settings() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(settings));
    }
}
