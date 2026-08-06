package net.everlastingness.client.agent;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

/**
 * Java Agent entry point. Used by <strong>modern</strong> Minecraft versions
 * (1.16.5+): the launcher starts the game with
 * {@code -javaagent:everlastingness-<version>.jar}, which invokes this
 * {@code premain} before {@code net.minecraft.client.main.Main} runs.
 *
 * <p>On <strong>legacy</strong> versions (1.7.10) the bootstrap happens through
 * the LaunchWrapper {@code ClientTweaker} instead — see that class in the
 * {@code v1_7_10} subproject. Both paths converge on {@link Bootstrap#start}.</p>
 *
 * <p>Note: actually installing the Mixin {@code ClassFileTransformer} here
 * requires a registered {@code IMixinService} (ModLauncher on modern versions).
 * Phase 0 wires the client-init half; full Mixin bootstrap is Phase 1.</p>
 */
public class EverlastingnessAgent {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/Agent");

    /**
     * JVM premain hook.
     *
     * @param args           agent arguments (unused in Phase 0)
     * @param instrumentation the JVM instrumentation handle, used in Phase 1 to
     *                        add the Mixin-backed {@code ClassFileTransformer}
     */
    public static void premain(String args, Instrumentation instrumentation) {
        String mcVersion = System.getProperty("everlastingness.version", "unknown");
        LOGGER.info("Everlastingness agent loaded (premain) for Minecraft " + mcVersion);

        // Bootstrap returns the singleton client; run module discovery on it.
        net.everlastingness.client.common.EverlastingnessClient client =
                net.everlastingness.client.common.Bootstrap.start(mcVersion);
        net.everlastingness.client.common.modules.ModuleDiscoverer.discoverAndRegister(client);

        if (instrumentation != null) {
            LOGGER.info("Instrumentation available — Mixin transformer will be registered in Phase 1");
        }
    }
}
