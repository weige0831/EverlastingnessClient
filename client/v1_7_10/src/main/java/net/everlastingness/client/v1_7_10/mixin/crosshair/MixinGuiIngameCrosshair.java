package net.everlastingness.client.v1_7_10.mixin.crosshair;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.everlastingness.client.modules.visual.CustomCrosshairModule;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Custom Crosshair Mixin — draws a custom crosshair overlay when enabled.
 * We inject at the HEAD of renderGameOverlay, draw our crosshair before
 * vanilla, then cancel the vanilla crosshair section. However, since
 * canceling mid-method is complex, we use a simpler approach: draw an
 * overlay on top.
 *
 * MCP name: renderGameOverlay(float,boolean,int,int) = func_73830_a
 */
@Mixin(GuiIngame.class)
public class MixinGuiIngameCrosshair {

    @Inject(remap = false, method = "func_73830_a(FZII)V", at = @At("RETURN"))
    private void everlastingness$drawCustomCrosshair(float partialTicks, boolean hasScreen,
            int mouseX, int mouseY, CallbackInfo ci) {
        EverlastingnessClient client = EverlastingnessClient.get();
        if (client == null) return;
        Module m = client.module("custom_crosshair");
        if (!(m instanceof CustomCrosshairModule) || !m.isEnabled()) return;

        CustomCrosshairModule mod = (CustomCrosshairModule) m;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        ScaledResolution res = new ScaledResolution(mc,
            mc.displayWidth, mc.displayHeight);
        int cx = res.getScaledWidth() / 2;
        int cy = res.getScaledHeight() / 2;

        // Simple cross-style crosshair with configurable color
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(mod.getSize());

        float a = ((mod.getColor() >> 24) & 0xFF) / 255.0f;
        float r = ((mod.getColor() >> 16) & 0xFF) / 255.0f;
        float g = ((mod.getColor() >> 8) & 0xFF) / 255.0f;
        float b = (mod.getColor() & 0xFF) / 255.0f;

        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_LINES);
        // Horizontal line
        GL11.glVertex2f(cx - mod.getSize(), cy);
        GL11.glVertex2f(cx + mod.getSize(), cy);
        // Vertical line
        GL11.glVertex2f(cx, cy - mod.getSize());
        GL11.glVertex2f(cx, cy + mod.getSize());
        GL11.glEnd();

        GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
