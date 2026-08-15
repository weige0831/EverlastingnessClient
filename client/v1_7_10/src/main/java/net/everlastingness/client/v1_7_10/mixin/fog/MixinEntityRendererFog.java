package net.everlastingness.client.v1_7_10.mixin.fog;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fog Mixin — cancels EntityRenderer.setupFog so no distance fog is applied,
 * mirroring Lunar's Fog "off" setting. The GL fog state left untouched means
 * fully clear distance rendering.
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRendererFog {

    @Inject(remap = false, method = "func_78468_a(IF)V", at = @At("HEAD"), cancellable = true)
    private void everlastingness$noFog(int renderPass, float partialTicks, CallbackInfo ci) {
        try {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client == null) return;
            Module m = client.module("fog");
            if (m != null && m.isEnabled()) {
                ci.cancel();
            }
        } catch (Throwable ignored) { }
    }
}
