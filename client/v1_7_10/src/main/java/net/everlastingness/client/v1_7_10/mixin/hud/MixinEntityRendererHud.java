package net.everlastingness.client.v1_7_10.mixin.hud;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.event.RenderTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Everlastingness HUD overlay for Minecraft 1.7.10.
 *
 * <p>Injects at the HEAD of {@code EntityRenderer.updateCameraAndRender(float)}
 * (the per-frame render entry, SRG {@code func_78480_b}) to:</p>
 * <ol>
 *   <li>post a {@link RenderTickEvent} onto the common event bus (so the
 *       version-agnostic HUD module can react); and</li>
 *   <li>draw the overlay directly here: player coordinates and FPS, top-left,
 *       using the game's own {@link FontRenderer}.</li>
 * </ol>
 *
 * <p>This is the first real, self-contained feature module: it proves the
 * injected client can read live game state (player position, fps/timer) and
 * paint visible text on screen every frame — the foundation of every Lunar /
 * Badlion-style HUD.</p>
 *
 * <p>MCP names verified against the decompiled 1.7.10 source: {@code mc} is a
 * private {@link Minecraft} field (shadowed here); {@code mc.fontRenderer},
 * {@code mc.thePlayer} (extends {@link Entity}, exposing {@code posX/Y/Z}),
 * {@code mc.getDebugFPS()} / {@code mc.timer} are all public.</p>
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRendererHud {

    /** The owning {@link Minecraft} instance on EntityRenderer. */
    @Shadow
    private Minecraft mc;

    /** Rolling FPS tracker — self-contained, no dependency on MC's debug fps. */
    private long lastFrameNanos = 0L;
    private double smoothedFps = 0.0;

    @Inject(method = "updateCameraAndRender", at = @At("HEAD"))
    private void everlastingness$drawHud(float partialTicks, CallbackInfo ci) {
        // 1. Notify the common event bus so version-agnostic modules can react.
        EverlastingnessClient client = EverlastingnessClient.get();
        if (client != null) {
            client.events().post(new RenderTickEvent(partialTicks));
        }

        // Update the rolling FPS estimate from the inter-frame delta.
        long now = System.nanoTime();
        if (lastFrameNanos != 0L) {
            double dtSeconds = (now - lastFrameNanos) / 1_000_000_000.0;
            if (dtSeconds > 0.0) {
                double instantFps = 1.0 / dtSeconds;
                // Exponential moving average smooths the displayed number.
                smoothedFps = smoothedFps == 0.0
                        ? instantFps
                        : smoothedFps * 0.9 + instantFps * 0.1;
            }
        }
        lastFrameNanos = now;

        // 2. Draw the overlay. Guard against null state during early load.
        if (mc == null || mc.fontRenderer == null || mc.thePlayer == null) {
            return;
        }

        FontRenderer font = mc.fontRenderer;
        Entity player = mc.thePlayer;

        String coords = String.format("XYZ: %.1f / %.1f / %.1f",
                player.posX, player.posY, player.posZ);
        String fps = String.format("FPS: %d", (int) Math.round(smoothedFps));

        // CPS line — read from CpsCounterModule if available
        String cps = "";
        EverlastingnessClient evClient = EverlastingnessClient.get();
        if (evClient != null) {
            net.everlastingness.client.common.module.Module cpsMod = evClient.module("cps_counter");
            if (cpsMod instanceof net.everlastingness.client.modules.input.CpsCounterModule) {
                net.everlastingness.client.modules.input.CpsCounterModule cpsM =
                    (net.everlastingness.client.modules.input.CpsCounterModule) cpsMod;
                if (cpsMod.isEnabled()) {
                    cps = String.format("CPS: [%d | %d]", cpsM.getLeftCps(), cpsM.getRightCps());
                }
            }
        }

        // Shadowed white text stacked vertically. 0xFFFFFFFF is opaque white.
        int x = 4;
        int y = 4;
        int color = 0xFFFFFFFF;
        font.drawStringWithShadow(coords, x, y, color);
        y += font.FONT_HEIGHT + 1;
        font.drawStringWithShadow(fps, x, y, color);
        if (!cps.isEmpty()) {
            y += font.FONT_HEIGHT + 1;
            font.drawStringWithShadow(cps, x, y, color);
        }
    }
}
