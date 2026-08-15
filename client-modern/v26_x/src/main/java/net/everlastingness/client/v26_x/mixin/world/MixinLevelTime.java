package net.everlastingness.client.v26_x.mixin.world;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class MixinLevelTime {
    @Inject(method = "getOverworldClockTime()J", at = @At("HEAD"), cancellable = true)
    private void everlastingness$overrideTime(CallbackInfoReturnable<Long> cir) {
        try {
            EverlastingnessClient c = EverlastingnessClient.get();
            if (c == null) return;
            var m = c.module("time_changer");
            if (m instanceof net.everlastingness.client.modules.visual.TimeChangerModule && m.isEnabled()) {
                cir.setReturnValue(((net.everlastingness.client.modules.visual.TimeChangerModule) m).getFixedTime());
            }
        } catch (Throwable ignored) { }
    }
}
