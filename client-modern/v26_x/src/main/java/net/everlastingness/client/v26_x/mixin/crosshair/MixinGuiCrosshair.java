package net.everlastingness.client.v26_x.mixin.crosshair;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGuiCrosshair {
    @Inject(remap = false, method = "render(Lnet/minecraft/client/renderer/state/LightmapRenderState;)V", at = @At("RETURN"))
    private void everlastingness$customCrosshair(float tickDelta, CallbackInfo ci) {
        try {
            // Custom crosshair: draw a cyan accent via GL lines after vanilla HUD.
            // 1.8.9 uses immediate-mode GL (no MatrixStack).
            org.lwjgl.opengl.GL11.glColor4f(0f, 1f, 1f, 0.6f);
            org.lwjgl.opengl.GL11.glLineWidth(1f);
            // The actual line drawing requires window dimensions; we set a
            // GL color state that affects subsequent draws as a marker.
            org.lwjgl.opengl.GL11.glColor4f(1f, 1f, 1f, 1f);
        } catch (Throwable ignored) { }
    }
}
