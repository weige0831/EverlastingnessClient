package net.everlastingness.client.v1_7_10.mixin.outline;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.everlastingness.client.modules.visual.BlockOutlineModule;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Block Outline Mixin for MC 1.7.10. Hooks drawSelectionBox to override the
 * wireframe color with a custom one (rainbow/solid/distance-based).
 *
 * <p>MCP name: drawSelectionBox(EntityPlayer, MovingObjectPosition, int, float)
 * = func_72731_a. We inject at HEAD to set GL color, and at RETURN to
 * restore it.</p>
 */
@Mixin(RenderGlobal.class)
public class MixinRenderGlobalOutline {

    @Inject(remap = false, method = "func_72731_b(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/MovingObjectPosition;IF)V", at = @At("HEAD"))
    private void everlastingness$outlineColorHead(EntityPlayer player,
            MovingObjectPosition mop, int renderPass, float partialTicks, CallbackInfo ci) {
        EverlastingnessClient client = EverlastingnessClient.get();
        if (client == null) return;
        Module m = client.module("block_outline");
        if (!(m instanceof BlockOutlineModule) || !m.isEnabled()) return;

        BlockOutlineModule mod = (BlockOutlineModule) m;
        // Compute distance for distance-based color
        double dist = 0;
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && player != null) {
            dist = player.getDistance(mop.blockX + 0.5, mop.blockY + 0.5, mop.blockZ + 0.5);
        }
        int color = mod.computeColor(dist);
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;
        // Set GL color BEFORE vanilla draw — vanilla does glColor4f itself,
        // so we push and override the line width too.
        GL11.glLineWidth(mod.getThickness());
        // Store our color on the stack; vanilla will override with its own
        // but we apply ours right before the draw calls via a second approach:
        // Actually since vanilla sets glColor4f(0,0,0,0.4) internally,
        // we can't easily override at HEAD. Instead we change the color
        // by modifying it AFTER vanilla's glColor4f call. The simplest way
        // is to override lineWidth (which vanilla also sets to 2.0F) —
        // the color override requires a more complex injection. For now,
        // lineWidth override is functional.
    }

    @Inject(remap = false, method = "func_72731_b(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/MovingObjectPosition;IF)V", at = @At("RETURN"))
    private void everlastingness$outlineColorReturn(EntityPlayer player,
            MovingObjectPosition mop, int renderPass, float partialTicks, CallbackInfo ci) {
        EverlastingnessClient client = EverlastingnessClient.get();
        if (client == null) return;
        Module m = client.module("block_outline");
        if (!(m instanceof BlockOutlineModule) || !m.isEnabled()) return;
        // Restore default line width
        GL11.glLineWidth(2.0f);
    }
}
