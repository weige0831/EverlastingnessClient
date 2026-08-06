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

        // Open the in-game config GUI with RIGHT_SHIFT (LWJGL Keyboard.KEY_RSHIFT = 60).
        // The GUI lists every registered module with a toggle button. Opening a
        // GuiScreen must happen on the main/render thread; the KeybindManager
        // callback runs on the game tick thread (runTick), which is correct.
        net.everlastingness.client.common.keybind.KeybindManager.get().register(
                "open_config_gui",
                60, // org.lwjgl.input.Keyboard.KEY_RSHIFT
                action -> {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                    if (mc != null && (mc.currentScreen == null)) {
                        mc.displayGuiScreen(
                                new net.everlastingness.client.v1_7_10.gui.ModuleConfigGuiScreen());
                        System.out.println("[Everlastingness] Config GUI opened (key RIGHT_SHIFT)");
                    }
                },
                "Open config GUI");

        // Register the local player for the default Everlastingness cape once a
        // session username is available. The cape module + MixinRenderPlayerCape
        // render the bundled texture for any registered player.
        try {
            String localName = net.minecraft.client.Minecraft.getMinecraft().getSession().getUsername();
            net.everlastingness.client.common.cosmetics.CosmeticsRegistry.get().setCape(
                    localName, net.everlastingness.client.common.cosmetics.CosmeticsRegistry.DEFAULT_CAPE);
        } catch (Throwable ignored) {
            // Session may not be ready at inject time; the cape simply won't show until re-register.
        }

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
