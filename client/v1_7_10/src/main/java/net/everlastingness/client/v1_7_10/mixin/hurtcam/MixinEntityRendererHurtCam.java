package net.everlastingness.client.v1_7_10.mixin.hurtcam;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hurt Cam Mixin — cancels the camera shake applied when the player takes
 * damage, mirroring Lunar's HurtCam "off" setting. Cancelling at HEAD skips
 * the entire GL rotation offset.
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRendererHurtCam {

    @Inject(remap = false, method = "func_78482_e(F)V", at = @At("HEAD"), cancellable = true)
    private void everlastingness$noHurtCam(float partialTicks, CallbackInfo ci) {
        try {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client == null) return;
            Module m = client.module("hurt_cam");
            if (m != null && m.isEnabled()) {
                ci.cancel();
            }
        } catch (Throwable ignored) { }
    }
}
