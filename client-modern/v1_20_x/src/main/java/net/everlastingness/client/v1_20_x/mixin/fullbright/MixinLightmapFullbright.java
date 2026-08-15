package net.everlastingness.client.v1_20_x.mixin.fullbright;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinLightmapFullbright {
    @Inject(remap = false, method = "updateLightmap(F)V", at = @At("HEAD"), cancellable = true)
    private void everlastingness$fullbright(float tickDelta, CallbackInfo ci) {
        // Cancel the lightmap update entirely — the lightmap stays at
        // its last (bright) state, giving fullbright effect.
        ci.cancel();
    }
}
