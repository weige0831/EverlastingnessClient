package net.everlastingness.client.v26_x.mixin.chunkborders;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.client.renderer.LevelRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRendererChunkBorders {
    @Inject(method = "render", at = @At("RETURN"))
    private void everlastingness$drawChunkBorders(CallbackInfo ci) {
        try {
            EverlastingnessClient c = EverlastingnessClient.get();
            if (c == null) return;
            var m = c.module("chunk_borders");
            if (m == null || !m.isEnabled()) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            int cx = (int) Math.floor(d(mc.player)) >> 4;
            int cz = (int) Math.floor(dz(mc.player)) >> 4;
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glColor4f(1f, 1f, 1f, 0.45f);
            GL11.glBegin(GL11.GL_LINES);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int bx = (cx + dx) << 4, bz = (cz + dz) << 4;
                    GL11.glVertex3i(bx, 0, bz); GL11.glVertex3i(bx, 256, bz);
                    GL11.glVertex3i(bx + 16, 0, bz); GL11.glVertex3i(bx + 16, 256, bz);
                    GL11.glVertex3i(bx, 0, bz + 16); GL11.glVertex3i(bx, 256, bz + 16);
                    GL11.glVertex3i(bx + 16, 0, bz + 16); GL11.glVertex3i(bx + 16, 256, bz + 16);
                }
            }
            GL11.glEnd();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } catch (Throwable ignored) { }
    }
    /** posX via getX() (modern) or field x (legacy). */
    private static double d(Object e) {
        try { return (Double) e.getClass().getMethod("getX").invoke(e); }
        catch (Throwable t) {
            try { Object v = e.getClass().getField("x").get(e);
                  return v instanceof Double ? (Double) v : ((Float) v).doubleValue(); }
            catch (Throwable ignored) { return 0.0; }
        }
    }
    private static double dz(Object e) {
        try { return (Double) e.getClass().getMethod("getZ").invoke(e); }
        catch (Throwable t) {
            try { Object v = e.getClass().getField("z").get(e);
                  return v instanceof Double ? (Double) v : ((Float) v).doubleValue(); }
            catch (Throwable ignored) { return 0.0; }
        }
    }
}
