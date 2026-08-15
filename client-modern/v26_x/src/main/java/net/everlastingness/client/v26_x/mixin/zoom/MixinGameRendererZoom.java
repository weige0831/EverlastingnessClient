package net.everlastingness.client.v26_x.mixin.zoom;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class MixinGameRendererZoom {

    @Inject(remap = false, method = "getFov(Lnet/minecraft/client/renderer/state/level/CameraRenderState;)F", at = @At("RETURN"), cancellable = true)
    private void everlastingness$zoom(float tickDelta, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        try {
            double target = ZoomState.isZoomActive() ? 1.0 : 0.0;
            double smooth = ZoomState.smoothTowards(target, 0.0, tickDelta);
            if (smooth > 0) cir.setReturnValue((float)(cir.getReturnValueF() * (1.0 - smooth * 0.7)));
        } catch (Throwable ignored) { }
    }
}
