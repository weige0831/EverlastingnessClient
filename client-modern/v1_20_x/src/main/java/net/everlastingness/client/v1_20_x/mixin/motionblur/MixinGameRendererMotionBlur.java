package net.everlastingness.client.v1_20_x.mixin.motionblur;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.client.render.GameRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRendererMotionBlur {
    private static int tex = -1, tw = -1, th = -1;

    @Inject(method = "render(FJZ)V", at = @At("RETURN"))
    private void everlastingness$motionBlur(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        try {
            EverlastingnessClient c = EverlastingnessClient.get();
            if (c == null) return;
            var m = c.module("motion_blur");
            if (!(m instanceof net.everlastingness.client.modules.visual.MotionBlurModule) || !m.isEnabled()) return;
            double amt = ((net.everlastingness.client.modules.visual.MotionBlurModule) m).getAmount();
            if (amt <= 0) return;
            Object win = net.minecraft.client.MinecraftClient.getInstance().getClass().getMethod("getWindow").invoke(net.minecraft.client.MinecraftClient.getInstance());
            int w = (Integer) win.getClass().getMethod("getFramebufferWidth").invoke(win);
            int h = (Integer) win.getClass().getMethod("getFramebufferHeight").invoke(win);
            if (tex == -1) tex = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
            if (w != tw || h != th) {
                tw = w; th = h;
                GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 0, 0, w, h, 0);
            } else {
                GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
            }
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1f, 1f, 1f, (float) amt);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0); GL11.glVertex2f(-1, -1);
            GL11.glTexCoord2f(1, 0); GL11.glVertex2f(1, -1);
            GL11.glTexCoord2f(1, 1); GL11.glVertex2f(1, 1);
            GL11.glTexCoord2f(0, 1); GL11.glVertex2f(-1, 1);
            GL11.glEnd();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            GL11.glDisable(GL11.GL_BLEND);
        } catch (Throwable ignored) { }
    }
}
