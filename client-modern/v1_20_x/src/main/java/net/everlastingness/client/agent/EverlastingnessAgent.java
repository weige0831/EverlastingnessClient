package net.everlastingness.client.agent;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

/**
 * Java Agent premain for the modern-era (1.20.x) Everlastingness client.
 *
 * <p>The launcher starts vanilla 1.20.x with
 * {@code -javaagent:everlastingness-1.20.1.jar}; this {@code premain} runs
 * before {@code net.minecraft.client.main.Main}.</p>
 *
 * <p>Phase 2 note: this currently only logs that the agent attached. Wiring
 * the full Mixin {@code ClassFileTransformer} (so the MixinGameRenderer hook
 * actually applies to the obfuscated runtime classes) requires solving the
 * intermediary→official reobf gap documented in the README — that is the next
 * engineering step. The manifest Premain-Class entry and this class establish
 * the agent contract the launcher expects.</p>
 */
public class EverlastingnessAgent {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/Agent");

    public static void premain(String args, Instrumentation instrumentation) {
        String mcVersion = System.getProperty("everlastingness.version", "1.20.x");
        LOGGER.info("Everlastingness agent attached (premain) for Minecraft " + mcVersion);

        if (instrumentation != null) {
            LOGGER.info("Instrumentation available — Mixin transformer registration pending " +
                    "intermediary→official reobf resolution (see README).");
        }
    }

    /** Agentmain entry for dynamic attach (same body as premain). */
    public static void agentmain(String args, Instrumentation instrumentation) {
        premain(args, instrumentation);
    }
}
