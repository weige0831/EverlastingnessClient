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

        // Register our notch remapper on the Mixin environment's remapper chain.
        // The @Mixin target annotations are already patched to notch string
        // form at build time (MixinTargetPatcher), so target classes resolve
        // directly. This IRemapper additionally covers any SRG class refs Mixin
        // resolves through descriptor parsing.
        try {
            org.spongepowered.asm.mixin.MixinEnvironment env =
                org.spongepowered.asm.mixin.MixinEnvironment.getDefaultEnvironment();
            env.getRemappers().add(new NotchRemapper());
            System.out.println("[Everlastingness] NotchRemapper registered on MixinEnvironment");
        } catch (Throwable t) {
            System.err.println("[Everlastingness] NotchRemapper env registration failed: " + t);
        }

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

        // Third batch — Lunar-parity HUD modules
        try { client.registerModule(new net.everlastingness.client.modules.hud.PingDisplayModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] Ping module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.hud.ArmorStatusModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] ArmorStatus module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.hud.DirectionHudModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] DirectionHud module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.hud.ServerAddressModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] ServerAddress module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.hud.MemoryUsageModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] MemoryUsage module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.hud.PlaytimeModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] Playtime module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.hud.KeystrokesModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] Keystrokes module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.hud.PotionEffectsModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] PotionEffects module: " + t.getMessage()); }

        // Fourth batch — Lunar-parity combat modules
        try { client.registerModule(new net.everlastingness.client.modules.combat.HitboxModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] Hitbox module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.combat.ReachDisplayModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] ReachDisplay module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.combat.ComboCounterModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] Combo module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.combat.DamageTintModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] DamageTint module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.combat.ToggleSneakModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] ToggleSneak module: " + t.getMessage()); }

        // Fifth batch — Lunar-parity performance/visual/QoL modules
        try { client.registerModule(new net.everlastingness.client.modules.performance.FogModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] Fog module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.performance.HurtCamModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] HurtCam module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.performance.ChunkBordersModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] ChunkBorders module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.visual.TimeChangerModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] TimeChanger module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.visual.WeatherChangerModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] WeatherChanger module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.visual.NickHiderModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] NickHider module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.visual.MotionBlurModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] MotionBlur module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.utility.AutoTextModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] AutoText module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.utility.ChatTimestampsModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] ChatTimestamps module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.utility.ScoreboardModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] Scoreboard module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.utility.ScreenshotModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] Screenshot module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.utility.ShulkerPreviewModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] ShulkerPreview module: " + t.getMessage()); }
        try { client.registerModule(new net.everlastingness.client.modules.utility.WailaModule()); }
        catch (Throwable t) { System.err.println("[Everlastingness] Waila module: " + t.getMessage()); }

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
