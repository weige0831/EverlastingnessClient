package net.everlastingness.client.v1_7_10.tweaker;

import net.everlastingness.client.common.Bootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * LaunchWrapper tweak class for the Everlastingness client on Minecraft 1.7.10.
 *
 * <p>This tweaker runs AFTER EverlastingnessPreTweaker (which bootstraps
 * Mixin). Its job is limited to starting the EverlastingnessClient singleton
 * and registering keybinds. It deliberately does NOT call ModuleDiscoverer
 * (ServiceLoader) here because that would load MC-referencing classes
 * (e.g. ModuleConfigGuiScreen extends GuiScreen) before LaunchClassLoader
 * has deobfuscated the MC jar. Module registration that references MC
 * classes is deferred to the mixins themselves (which run per-frame, after
 * MC classes are available).</p>
 */
public class ClientTweaker implements net.minecraft.launchwrapper.ITweaker {

    private final List<String> args = new ArrayList<>();

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        this.args.addAll(args);
    }

    @Override
    public void injectIntoClassLoader(net.minecraft.launchwrapper.LaunchClassLoader classLoader) {
        String mcVersion = System.getProperty("everlastingness.version", "1.7.10");
        System.out.println("[Everlastingness] ClientTweaker injecting for " + mcVersion);

        // Start the client core (no MC references in :common, safe here).
        net.everlastingness.client.common.EverlastingnessClient client = Bootstrap.start(mcVersion);

        // Register HUD module directly (no ServiceLoader — avoids loading
        // MC-referencing classes prematurely).
        try {
            net.everlastingness.client.common.module.Module hud =
                new net.everlastingness.client.modules.hud.HudOverlayModule();
            client.registerModule(hud);
        } catch (Throwable t) {
            System.err.println("[Everlastingness] HUD module registration deferred: " + t.getMessage());
        }

        try {
            net.everlastingness.client.common.module.Module cape =
                new net.everlastingness.client.modules.cape.CapeModule();
            client.registerModule(cape);
        } catch (Throwable t) {
            System.err.println("[Everlastingness] Cape module registration deferred: " + t.getMessage());
        }

        try {
            net.everlastingness.client.common.module.Module fps =
                new net.everlastingness.client.modules.fps.FpsOptimizationModule();
            client.registerModule(fps);
        } catch (Throwable t) {
            System.err.println("[Everlastingness] FPS module registration deferred: " + t.getMessage());
        }

        // Keybinds: register toggle_hud (R) and open_config_gui (RIGHT_SHIFT).
        // These are deferred to when the mixin fires (MC classes available).
        System.out.println("[Everlastingness] ClientTweaker complete — modules registered");
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.Minecraft";
    }

    @Override
    public String[] getLaunchArguments() {
        return args.toArray(new String[0]);
    }
}
