package net.everlastingness.client.v1_20_x.mixin.zoom;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class MixinGameRendererZoom {

    @Inject(remap = false, method = "getFov(FZ)F", at = @At("RETURN"), cancellable = true)
    private void everlastingness$zoom(float tickDelta, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        try {
            double target = ZoomState.isZoomActive() ? 1.0 : 0.0;
            double smooth = ZoomState.smoothTowards(target, 0.0, tickDelta);
            if (smooth > 0) cir.setReturnValue((float)(cir.getReturnValueF() * (1.0 - smooth * 0.7)));
        } catch (Throwable ignored) { }
    }
}
