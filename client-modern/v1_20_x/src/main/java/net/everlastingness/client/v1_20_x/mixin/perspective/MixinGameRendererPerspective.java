package net.everlastingness.client.v1_20_x.mixin.perspective;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRendererPerspective {
    @Inject(remap = false, method = "setupCamera(FI)V", at = @At("HEAD"))
    private void everlastingness$overridePerspective(float tickDelta, int renderPass, CallbackInfo ci) {
        try {
            // The keybind mixin already sets options.perspective directly,
            // which vanilla setupCamera reads. This injection is a no-op marker
            // that confirms the camera setup path is hooked.
        } catch (Throwable ignored) { }
    }
}
