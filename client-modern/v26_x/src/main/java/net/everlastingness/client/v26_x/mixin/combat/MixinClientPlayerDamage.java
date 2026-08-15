package net.everlastingness.client.v26_x.mixin.combat;

import net.everlastingness.client.common.EverlastingnessClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
        "net.minecraft.client.player.LocalPlayer",
        "net.minecraft.client.player.LocalPlayer" }, remap = false)
public class MixinClientPlayerDamage {
    @Inject(method = "hurt", at = @At("HEAD"), require = 0, remap = false)
    private void everlastingness$onHurt(Object source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        try {
            EverlastingnessClient c = EverlastingnessClient.get();
            if (c == null) return;
            var cb = c.module("combo_counter");
            if (cb instanceof net.everlastingness.client.modules.combat.ComboCounterModule && cb.isEnabled()) {
                ((net.everlastingness.client.modules.combat.ComboCounterModule) cb).onHurt();
            }
            var dt = c.module("damage_tint");
            if (dt != null && dt.isEnabled()) {
                DamageTintState.markHurt();
            }
        } catch (Throwable ignored) { }
    }
}
