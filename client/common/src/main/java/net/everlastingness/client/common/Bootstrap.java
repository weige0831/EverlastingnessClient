package net.everlastingness.client.common;

import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bootstrap entry point shared by both injection paths:
 * <ul>
 *   <li>Legacy (1.7.10): the LaunchWrapper {@code ClientTweaker} calls
 *       {@link #start} after {@code MixinBootstrap} is up.</li>
 *   <li>Modern (1.16.5+): the Java Agent {@code premain} calls it before
 *       {@code net.minecraft.client.main.Main} runs.</li>
 * </ul>
 *
 * <p>Creates the {@link EverlastingnessClient} singleton and runs module
 * discovery. Idempotent — safe to call from either path.</p>
 */
public final class Bootstrap {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/Bootstrap");
    private static volatile boolean started;

    private Bootstrap() {
    }

    /** Initialise the client singleton for the given Minecraft version. */
    public static EverlastingnessClient start(String minecraftVersion) {
        EverlastingnessClient client = EverlastingnessClient.init(minecraftVersion);
        if (!started) {
            started = true;
            // JVM shutdown hook ensures modules get a clean onDisable().
            Runtime.getRuntime().addShutdownHook(new Thread(client::shutdown,
                    "Everlastingness-Shutdown"));
        }
        return client;
    }

    /** Whether {@link #start} has been called. */
    public static boolean isStarted() {
        return started;
    }
}
