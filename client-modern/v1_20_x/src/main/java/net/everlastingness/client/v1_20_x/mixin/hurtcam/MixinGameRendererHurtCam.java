package net.everlastingness.client.v1_20_x.mixin.hurtcam;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRendererHurtCam {
    @Inject(method = "tiltViewWhenHurt(F)V", at = @At("HEAD"), cancellable = true)
    private void everlastingness$noHurtCam(float delta, CallbackInfo ci) {
        try {
            EverlastingnessClient c = EverlastingnessClient.get();
            if (c == null) return;
            var m = c.module("hurt_cam");
            if (m != null && m.isEnabled()) ci.cancel();
        } catch (Throwable ignored) { }
    }
}
