package net.everlastingness.client.v1_7_10.mixin.autoworld;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Test-only Mixin: when the system property {@code everlastingness.autoworld}
 * is set to {@code true}, this Mixin auto-creates and joins a fresh
 * singleplayer world once the main menu has finished rendering at least one
 * frame. Used by the headless test harness to drive the game into an in-world
 * state (where HUD/Fullbright/Crosshair/etc. render) without needing GUI mouse
 * automation.
 *
 * <p>We inject into GuiMainMenu's {@code drawScreen} ({@code func_73863_a},
 * which runs every frame the menu is visible) and launch on the FIRST frame —
 * by which point the game loop is fully spun up and all subsystems (texture
 * manager, sound, etc.) are initialised, avoiding the NPE that occurs if we
 * launch from {@code initGui} (which fires before the first render).</p>
 *
 * <p>The world is created with a fixed seed, survival mode, default world
 * type, so each run is reproducible.</p>
 */
@Mixin(GuiMainMenu.class)
public class MixinAutoWorld {

    private static boolean launched = false;

    @Inject(remap = false, method = "func_73863_a(IIF)V", at = @At("RETURN"))
    private void everlastingness$autoLaunch(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (launched) return;
        String flag = System.getProperty("everlastingness.autoworld", "false");
        if (!"true".equalsIgnoreCase(flag)) return;
        launched = true;

        System.out.println("[Everlastingness] AutoWorld: launching fresh singleplayer world (first frame)");
        try {
            Minecraft mc = Minecraft.getMinecraft();
            // WorldSettings: (seed, gamemode, generateMap, hardcore, worldType)
            long seed = 12345L;
            WorldSettings settings = new WorldSettings(seed, WorldSettings.GameType.SURVIVAL, true, false, WorldType.DEFAULT);
            // launchIntegratedServer(folderName, worldName, settings) = func_71371_a
            // In the MCP dev env the method is launchIntegratedServer; RFG reobf
            // will remap it to func_71371_a for the production jar.
            mc.launchIntegratedServer("EverlastingnessTest", "Everlastingness Test World", settings);
            System.out.println("[Everlastingness] AutoWorld: world launched");
        } catch (Throwable t) {
            System.err.println("[Everlastingness] AutoWorld failed: " + t);
            t.printStackTrace();
        }
    }
}


