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
        // args contains the non-option arguments after Launch.launch() parsed
        // --tweakClass, --version, --gameDir, --assetsDir. We need to rebuild
        // the full argument list for Main.main() which expects ALL options.
        this.args.addAll(args);
        // Re-add the standard options that Launch.launch() consumed.
        if (gameDir != null) {
            this.args.add("--gameDir");
            this.args.add(gameDir.getAbsolutePath());
        }
        if (assetsDir != null) {
            this.args.add("--assetsDir");
            this.args.add(assetsDir.getAbsolutePath());
        }
        // version is passed as the profile parameter in some versions
        if (profile != null && !profile.isEmpty()) {
            this.args.add("--version");
            this.args.add(profile);
        }
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

        // New modules — matching Lunar/Badlion feature matrix
        try {
            client.registerModule(new net.everlastingness.client.modules.input.CpsCounterModule());
        } catch (Throwable t) { System.err.println("[Everlastingness] CPS module: " + t.getMessage()); }

        try {
            client.registerModule(new net.everlastingness.client.modules.visual.FullbrightModule());
        } catch (Throwable t) { System.err.println("[Everlastingness] Fullbright module: " + t.getMessage()); }

        try {
            client.registerModule(new net.everlastingness.client.modules.camera.PerspectiveModule());
        } catch (Throwable t) { System.err.println("[Everlastingness] Perspective module: " + t.getMessage()); }

        try {
            client.registerModule(new net.everlastingness.client.modules.visual.CustomCrosshairModule());
        } catch (Throwable t) { System.err.println("[Everlastingness] Crosshair module: " + t.getMessage()); }

        try {
            client.registerModule(new net.everlastingness.client.modules.visual.BlockOutlineModule());
        } catch (Throwable t) { System.err.println("[Everlastingness] Outline module: " + t.getMessage()); }

        // Second batch — matching more Lunar/Badlion features
        try {
            client.registerModule(new net.everlastingness.client.modules.hud.ClockArmorHudModule());
        } catch (Throwable t) { System.err.println("[Everlastingness] Clock&Armor module: " + t.getMessage()); }

        try {
            client.registerModule(new net.everlastingness.client.modules.utility.CoordinateCopyModule());
        } catch (Throwable t) { System.err.println("[Everlastingness] CoordCopy module: " + t.getMessage()); }

        try {
            client.registerModule(new net.everlastingness.client.modules.input.SmoothScrollModule());
        } catch (Throwable t) { System.err.println("[Everlastingness] SmoothScroll module: " + t.getMessage()); }

        try {
            client.registerModule(new net.everlastingness.client.modules.visual.ZoomModule());
        } catch (Throwable t) { System.err.println("[Everlastingness] Zoom module: " + t.getMessage()); }

        // Keybinds: register toggle_hud (R) and open_config_gui (RIGHT_SHIFT).
        // These are deferred to when the mixin fires (MC classes available).
        System.out.println("[Everlastingness] ClientTweaker complete — modules registered");
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        // Return the stored args (vanilla game args captured during acceptOptions).
        // Launch.launch() merges these from all tweakers.
        return args.toArray(new String[0]);
    }
}
