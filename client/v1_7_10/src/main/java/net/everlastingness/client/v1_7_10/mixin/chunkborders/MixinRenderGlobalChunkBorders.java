package net.everlastingness.client.v1_7_10.mixin.chunkborders;
import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * Chunk Borders Mixin — draws the 16-block chunk grid around the player
 * after the world finishes rendering, mirroring Lunar's ChunkBorders.
 *
 * <p>Target: RenderGlobal.drawBlockDamageTexture = func_72717_a (runs each
 * frame with world-space GL state active). We draw thin white line loops at
 * the chunk borders around the player's current chunk plus the 8 neighbours,
 * from y=0 to the world height, using the vanilla Tessellator GL_LINES mode.</p>
 */
@Mixin(RenderGlobal.class)
public class MixinRenderGlobalChunkBorders {
    @Inject(remap = false,
            method = "func_72717_a(Lnet/minecraft/client/renderer/Tessellator;Lnet/minecraft/entity/player/EntityPlayer;F)V",
            at = @At("HEAD"))
    private void everlastingness$drawChunkBorders(net.minecraft.client.renderer.Tessellator tess,
            EntityPlayer player, float partialTicks, CallbackInfo ci) {
        try {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client == null) return;
            Module m = client.module("chunk_borders");
            if (m == null || !m.isEnabled() || player == null) return;
            int px = (int) Math.floor(player.posX);
            int pz = (int) Math.floor(player.posZ);
            int cx = px >> 4;
            int cz = pz >> 4;
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.45f);
            GL11.glLineWidth(1.5f);
            GL11.glBegin(GL11.GL_LINES);
            int minY = 0;
            int maxY = 256;
            // The player's chunk border plus the 8 surrounding chunks.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int baseX = (cx + dx) << 4;
                    int baseZ = (cz + dz) << 4;
                    // Four vertical edges of the chunk.
                    vline(baseX, minY, maxY, baseZ);
                    vline(baseX + 16, minY, maxY, baseZ);
                    vline(baseX, minY, maxY, baseZ + 16);
                    vline(baseX + 16, minY, maxY, baseZ + 16);
                    // Top and bottom rings.
                    ring(baseX, baseZ, minY);
                    ring(baseX, baseZ, maxY);
                }
            }
            GL11.glEnd();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_BLEND);
        } catch (Throwable ignored) { }
    }
    private static void vline(int x, int y0, int y1, int z) {
        GL11.glVertex3i(x, y0, z);
        GL11.glVertex3i(x, y1, z);
    }
    private static void ring(int x, int z, int y) {
        GL11.glVertex3i(x, y, z);
        GL11.glVertex3i(x + 16, y, z);
        GL11.glVertex3i(x + 16, y, z);
        GL11.glVertex3i(x + 16, y, z + 16);
        GL11.glVertex3i(x + 16, y, z + 16);
        GL11.glVertex3i(x, y, z + 16);
        GL11.glVertex3i(x, y, z + 16);
        GL11.glVertex3i(x, y, z);
    }
}
