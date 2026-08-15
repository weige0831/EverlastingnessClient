package net.everlastingness.client.v26_x.mixin.scoreboard;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGuiScoreboard {
    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void everlastingness$hideSidebar(CallbackInfo ci) {
        try {
            EverlastingnessClient c = EverlastingnessClient.get();
            if (c == null) return;
            var m = c.module("scoreboard");
            if (m instanceof net.everlastingness.client.modules.utility.ScoreboardModule
                    && m.isEnabled()
                    && ((net.everlastingness.client.modules.utility.ScoreboardModule) m).isHidingSidebar()) {
                ci.cancel();
            }
        } catch (Throwable ignored) { }
    }
}
