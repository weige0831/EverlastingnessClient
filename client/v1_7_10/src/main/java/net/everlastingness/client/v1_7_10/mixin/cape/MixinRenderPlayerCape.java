package net.everlastingness.client.v1_7_10.mixin.cape;

import net.everlastingness.client.common.cosmetics.CosmeticsRegistry;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Everlastingness cape module for Minecraft 1.7.10.
 *
 * <p>Injects at RETURN of {@code RenderPlayer.doRender(AbstractClientPlayer,...)}
 * and, when cosmetics are enabled and the rendered player has a registered
 * custom cape, draws a simple cape quad behind them using the bundled
 * Everlastingness cape texture. This demonstrates the cosmetics render path:
 * per-player texture selection + GL drawing via MC's {@link Tessellator}.</p>
 *
 * <p>This draws our OWN cape quad rather than hijacking vanilla's cape logic, so
 * it composes cleanly with (or without) the player's vanilla cape. The quad is
 * a small vertical plane offset behind the player body, lit fully bright so the
 * texture is readable — a simplified stand-in for the full cape-physics mesh
 * that a production cosmetics layer would animate.</p>
 *
 * <p>MCP names verified against the decompiled 1.7.10 source: doRender line
 * 138, bindTexture (Render), Tessellator.instance/startDrawingQuads/
 * addVertexWithUV/draw, Entity.getCommandSenderName.</p>
 */
@Mixin(RenderPlayer.class)
public class MixinRenderPlayerCape {

    /** The bundled default cape texture (a ResourceLocation in our namespace). */
    private static final ResourceLocation CAPE_TEXTURE =
            new ResourceLocation("everlastingness", "textures/capes/default.png");

    @Inject(remap = false, 
        method = "func_76986_a(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
        at = @At("RETURN"))
    private void everlastingness$drawCape(AbstractClientPlayer player,
                                          double x, double y, double z,
                                          float entityYaw, float partialTicks,
                                          CallbackInfo ci) {
        CosmeticsRegistry cosmetics = CosmeticsRegistry.get();
        if (!cosmetics.isEnabled() || player == null) {
            return;
        }

        // Only render a custom cape for players registered with the default
        // Everlastingness cape (later: per-player textures from a backend).
        String name = null;
        try {
            name = player.getCommandSenderName();
        } catch (Throwable ignored) {
            return;
        }
        String capeId = cosmetics.getCape(name);
        if (capeId == null) {
            return; // no custom cape for this player — leave vanilla rendering alone
        }

        drawCapeQuad(player, x, y, z, partialTicks);
    }

    /**
     * Draw a simple cape quad behind the player. Lit fully bright (GL lighting
     * disabled) so the texture colour shows. The quad follows the player's yaw
     * roughly, hanging off the back.
     */
    private void drawCapeQuad(AbstractClientPlayer player, double x, double y, double z, float partialTicks) {
        RenderPlayer self = (RenderPlayer) (Object) this;
        // Bind our cape texture via the renderer's own texture binding helper.
        try {
            // bindTexture is protected on Render; reflect-invocation is avoided by
            // using the public Minecraft render engine through the field. As a
            // portable fallback, bind via GL directly using the texture manager.
            // MC 1.7.10 Render.bindTexture delegates to TextureManager; we call it
            // through reflection-safe public path:
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc != null && mc.getTextureManager() != null) {
                mc.getTextureManager().bindTexture(CAPE_TEXTURE);
            }
        } catch (Throwable t) {
            return;
        }

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        // Orient roughly with the player's body yaw.
        float yawOffset = player.prevRenderYawOffset
                + (player.renderYawOffset - player.prevRenderYawOffset) * partialTicks;
        GL11.glRotatef(180.0F - yawOffset, 0.0F, 1.0F, 0.0F);

        // Position the quad just behind the body, centered vertically on the torso.
        GL11.glTranslatef(0.0F, 0.0F, 0.18F);

        // Full bright: disable lighting so the cape texture isn't darkened.
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // A 10-wide, 16-tall quad (roughly the cape's aspect), top anchored ~0.3 below player y.
        float w = 0.5F;
        float h = 0.8F;
        float top = -0.3F;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(-w, top, 0.0, 0.0, 0.0);
        t.addVertexWithUV(w, top, 0.0, 1.0, 0.0);
        t.addVertexWithUV(w, top - h, 0.0, 1.0, 1.0);
        t.addVertexWithUV(-w, top - h, 0.0, 0.0, 1.0);
        t.draw();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }
}
