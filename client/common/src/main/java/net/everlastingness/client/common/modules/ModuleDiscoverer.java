package net.everlastingness.client.common.modules;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;

import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Discovers feature {@link Module}s via Java's {@link ServiceLoader}. Each
 * module registers itself through a
 * {@code META-INF/services/net.everlastingness.client.common.module.Module}
 * file in its jar; this keeps the bootstrap decoupled from concrete modules.
 */
public final class ModuleDiscoverer {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/Modules");

    private ModuleDiscoverer() {
    }

    /** Load all modules visible to the context classloader and register them. */
    public static void discoverAndRegister(EverlastingnessClient client) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ModuleDiscoverer.class.getClassLoader();
        }
        ServiceLoader<Module> loader = ServiceLoader.load(Module.class, cl);
        int count = 0;
        for (Module module : loader) {
            try {
                client.registerModule(module);
                count++;
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Failed to register module " + module.getClass(), t);
            }
        }
        LOGGER.info("Discovered and registered " + count + " module(s)");
    }
}
