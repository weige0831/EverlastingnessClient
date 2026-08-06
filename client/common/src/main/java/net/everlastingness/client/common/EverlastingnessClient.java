package net.everlastingness.client.common;

import net.everlastingness.client.common.config.ClientConfig;
import net.everlastingness.client.common.module.Module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Everlastingness client core. A single instance is created by the
 * bootstrap component ({@link Bootstrap} — either the LaunchWrapper
 * {@code ClientTweaker} on legacy versions or the Java Agent on modern
 * versions) once Mixin is wired up.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Hold the {@link EventBus} (shared, process-wide).</li>
 *   <li>Discover and lifecycle-manage feature {@link Module}s.</li>
 *   <li>Provide global logging and configuration access.</li>
 * </ul>
 */
public final class EverlastingnessClient {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness");
    private static EverlastingnessClient instance;

    private final EventBus eventBus = EventBus.get();
    private final ClientConfig config = new ClientConfig();
    private final Map<String, Module> modules = new LinkedHashMap<>();
    private final String minecraftVersion;

    private EverlastingnessClient(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    /** Initialise the singleton for the running Minecraft version. */
    public static EverlastingnessClient init(String minecraftVersion) {
        if (instance != null) {
            LOGGER.warning("EverlastingnessClient already initialised; ignoring re-init");
            return instance;
        }
        instance = new EverlastingnessClient(minecraftVersion);
        LOGGER.info("Everlastingness client initialising for Minecraft " + minecraftVersion);
        return instance;
    }

    /** The singleton, or null if not yet initialised. */
    public static EverlastingnessClient get() {
        return instance;
    }

    public EventBus events() {
        return eventBus;
    }

    public ClientConfig config() {
        return config;
    }

    public String minecraftVersion() {
        return minecraftVersion;
    }

    /** Register a module and enable it if its config/default says so. */
    public void registerModule(Module module) {
        modules.put(module.getId(), module);
        boolean shouldEnable = config.getModuleEnabled(module.getId(), module.isEnabledByDefault());
        if (shouldEnable) {
            safeEnable(module);
        }
    }

    /** Enable a registered module by id. */
    public void enableModule(String id) {
        Module m = modules.get(id);
        if (m != null) {
            safeEnable(m);
            config.setModuleEnabled(id, true);
        }
    }

    /** Disable a registered module by id. */
    public void disableModule(String id) {
        Module m = modules.get(id);
        if (m != null) {
            m.disable();
            config.setModuleEnabled(id, false);
        }
    }

    /** Immutable view of registered modules. */
    public Collection<Module> modules() {
        return Collections.unmodifiableCollection(new ArrayList<>(modules.values()));
    }

    /** Look up a module by id. */
    public Module module(String id) {
        return modules.get(id);
    }

    /** Disable all modules (called on shutdown). */
    public void shutdown() {
        List<Module> snapshot = new ArrayList<>(modules.values());
        Collections.reverse(snapshot);
        for (Module m : snapshot) {
            try {
                m.disable();
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Error disabling module " + m.getId(), t);
            }
        }
    }

    private void safeEnable(Module module) {
        try {
            module.enable();
            LOGGER.info("Enabled module: " + module.getId());
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Failed to enable module " + module.getId(), t);
        }
    }
}
