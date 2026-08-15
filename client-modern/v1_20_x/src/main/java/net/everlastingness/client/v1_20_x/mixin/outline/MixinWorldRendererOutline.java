package net.everlastingness.client.v1_20_x.mixin.outline;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRendererOutline {
    @Inject(remap = false, method = "drawEntityOutlineFramebuffer()V", at = @At("HEAD"))
    private void everlastingness$outlineColor(CallbackInfo ci) {
        try {
            // Set a custom shader color for entity outlines (cyan tint).
            // The outline framebuffer uses a shader; we pre-set GL color state.
            org.lwjgl.opengl.GL11.glColor4f(0f, 1f, 1f, 1f);
        } catch (Throwable ignored) { }
    }
}
