package net.everlastingness.client.v1_20_x.mixin.fps;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FPS optimization Mixin — entity-distance culling. String-targets whichever
 * render dispatcher exists in the current MC version (EntityRenderDispatcher
 * from 1.8.9 through 1.21.8); require=0 keeps missing targets non-fatal.
 */
@Mixin(targets = "net.minecraft.client.render.entity.EntityRenderDispatcher", remap = false)
public class MixinEntityRenderDispatcherFps {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void everlastingness$cull(Object entity, double x, double y, double z,
            float yaw, float tickDelta, Object matrices, Object vcp, int light,
            CallbackInfoReturnable<Boolean> cir) {
        try {
            if (x * x + y * y + z * z > 4096.0) cir.setReturnValue(false);
        } catch (Throwable ignored) { }
    }
}
