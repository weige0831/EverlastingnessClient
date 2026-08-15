package net.everlastingness.client.v1_7_10.mixin.scoreboard;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scoreboard Mixin — cancels the sidebar scoreboard render when the module's
 * hideSidebar setting is on, mirroring Lunar's Scoreboard toggle.
 *
 * <p>Target: GuiIngame.func_96136_a (renderScoreboard sidebar).</p>
 */
@Mixin(GuiIngame.class)
public class MixinGuiIngameScoreboard {

    @Inject(remap = false,
            method = "func_96136_a(Lnet/minecraft/scoreboard/ScoreObjective;IILnet/minecraft/client/gui/FontRenderer;)V",
            at = @At("HEAD"), cancellable = true)
    private void everlastingness$hideScoreboard(ScoreObjective objective, int x, int y,
            net.minecraft.client.gui.FontRenderer font, CallbackInfo ci) {
        try {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client == null) return;
            Module m = client.module("scoreboard");
            if (m instanceof net.everlastingness.client.modules.utility.ScoreboardModule
                    && m.isEnabled()
                    && ((net.everlastingness.client.modules.utility.ScoreboardModule) m).isHidingSidebar()) {
                ci.cancel();
            }
        } catch (Throwable ignored) { }
    }
}
