package net.everlastingness.client.v1_7_10.tweaker;

import net.everlastingness.client.common.Bootstrap;
import net.everlastingness.client.common.modules.ModuleDiscoverer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * LaunchWrapper tweak class for the Everlastingness client on Minecraft
 * 1.7.10. The launcher starts vanilla through
 * {@code net.minecraft.launchwrapper.Launch} with {@code --tweakClass} set to
 * (a) {@code org.spongepowered.asm.launch.MixinTweaker} and (b) this class.
 *
 * <p>LaunchWrapper instantiates tweakers in declaration order, so by the time
 * {@link #injectIntoClassLoader(LaunchClassLoader)} is called the Mixin
 * environment is already bootstrapped and we can safely start the client.</p>
 *
 * <p>This mirrors the architecture used by Forge, OptiFine and Badlion on the
 * 1.7–1.12 era: a custom {@code ITweaker} layered on top of LaunchWrapper.</p>
 */
public class ClientTweaker implements ITweaker {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/Tweaker");

    private final List<String> args = new ArrayList<>();

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        // Capture LaunchWrapper args for later use by the game entry point.
        this.args.addAll(args);
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        String mcVersion = System.getProperty("everlastingness.version", "1.7.10");
        LOGGER.info("Everlastingness ClientTweaker injecting into classloader for " + mcVersion);

        // Start the client core (creates the singleton + event bus) and run
        // module discovery via the ServiceLoader in :common.
        net.everlastingness.client.common.EverlastingnessClient client =
                Bootstrap.start(mcVersion);
        ModuleDiscoverer.discoverAndRegister(client);

        // Register a demonstration keybind: pressing R (LWJGL Keyboard.KEY_R = 19)
        // in-game logs a message and toggles the HUD module on/off. This proves
        // the KeybindManager end-to-end (registry → per-tick poll → callback).
        net.everlastingness.client.common.keybind.KeybindManager.get().register(
                "toggle_hud",
                19, // org.lwjgl.input.Keyboard.KEY_R
                action -> {
                    net.everlastingness.client.common.module.Module hud = client.module("hud");
                    if (hud != null) {
                        if (hud.isEnabled()) {
                            client.disableModule("hud");
                        } else {
                            client.enableModule("hud");
                        }
                        System.out.println("[Everlastingness] HUD " +
                                (hud.isEnabled() ? "enabled" : "disabled") + " (key R)");
                    } else {
                        System.out.println("[Everlastingness] Key R pressed (no HUD module)");
                    }
                },
                "Toggle HUD overlay");

        // Per-version mixins (see src/main/java/.../mixin/) are picked up by
        // MixinTweaker via the mixins.everlastingness.json config, which the
        // launcher sets with -Dmixin.configs.
    }

    @Override
    public String getLaunchTarget() {
        // Vanilla 1.7.10 entry class. LaunchWrapper delegates to it after all
        // tweakers have injected.
        return "net.minecraft.client.Minecraft";
    }

    @Override
    public String[] getLaunchArguments() {
        return args.toArray(new String[0]);
    }
}
