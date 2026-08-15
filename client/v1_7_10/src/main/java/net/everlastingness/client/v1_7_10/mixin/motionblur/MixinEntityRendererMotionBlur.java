package net.everlastingness.client.v1_7_10.mixin.motionblur;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.client.renderer.EntityRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Motion Blur Mixin — after the world renders, blends the previous frame
 * over the current one for a subtle motion blur, mirroring Lunar's MotionBlur.
 *
 * <p>Implementation: copies the framebuffer into a display list-free approach —
 * uses GL accumulation-style blending by drawing the prior frame texture with
 * alpha = module amount. To stay allocation-free we keep one texture id and
 * re-download the frame each pass (copyTexImage2D then draw).</p>
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRendererMotionBlur {

    private static int blurTextureId = -1;
    private static int blurWidth = -1;
    private static int blurHeight = -1;

    @Inject(remap = false, method = "func_78480_b(F)V", at = @At("RETURN"))
    private void everlastingness$motionBlur(float partialTicks, CallbackInfo ci) {
        try {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client == null) return;
            Module m = client.module("motion_blur");
            if (!(m instanceof net.everlastingness.client.modules.visual.MotionBlurModule)
                    || !m.isEnabled()) return;
            double amount = ((net.everlastingness.client.modules.visual.MotionBlurModule) m).getAmount();
            if (amount <= 0.0) return;

            int w = org.lwjgl.opengl.Display.getWidth();
            int h = org.lwjgl.opengl.Display.getHeight();

            // (Re)allocate the blur texture at the current size.
            if (blurTextureId == -1) {
                blurTextureId = GL11.glGenTextures();
            }
            if (w != blurWidth || h != blurHeight) {
                blurWidth = w;
                blurHeight = h;
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurTextureId);
                GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 0, 0, w, h, 0);
            } else {
                // Refresh the stored frame.
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurTextureId);
                GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
            }

            // Draw the previous frame over the current one with the blend
            // amount — the larger the amount, the stronger the trail.
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1f, 1f, 1f, (float) amount);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0f, 0f); GL11.glVertex2f(-1f, -1f);
            GL11.glTexCoord2f(1f, 0f); GL11.glVertex2f(1f, -1f);
            GL11.glTexCoord2f(1f, 1f); GL11.glVertex2f(1f, 1f);
            GL11.glTexCoord2f(0f, 1f); GL11.glVertex2f(-1f, 1f);
            GL11.glEnd();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            GL11.glDisable(GL11.GL_BLEND);
        } catch (Throwable ignored) { }
    }
}
