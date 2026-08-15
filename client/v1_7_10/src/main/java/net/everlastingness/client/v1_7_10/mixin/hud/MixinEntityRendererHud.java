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
    @Shadow(aliases = {"field_78531_r"}, remap = false)
    private Minecraft mc;

    /** Rolling FPS tracker — self-contained, no dependency on MC's debug fps. */
    private long lastFrameNanos = 0L;
    private double smoothedFps = 0.0;

    @Inject(remap = false, method = "func_78480_b(F)V", at = @At("HEAD"))
    private void everlastingness$drawHud(float partialTicks, CallbackInfo ci) {
        try {
            drawHudImpl(partialTicks);
        } catch (Throwable t) {
            // Never let HUD rendering crash the game loop. Print full stack once.
            if (!hudErrorLogged) {
                hudErrorLogged = true;
                System.err.println("[Everlastingness] HUD render error: " + t);
                t.printStackTrace();
            }
        }
    }

    private static boolean hudErrorLogged = false;

    private void drawHudImpl(float partialTicks) {
        // 1. Notify the common event bus so version-agnostic modules can react.
        try {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client != null && client.events() != null) {
                client.events().post(new RenderTickEvent(partialTicks));
            }
        } catch (Throwable ignored) { }

        // Update the rolling FPS estimate from the inter-frame delta.
        try {
            long now = System.nanoTime();
            if (lastFrameNanos != 0L) {
                double dtSeconds = (now - lastFrameNanos) / 1_000_000_000.0;
                if (dtSeconds > 0.0) {
                    double instantFps = 1.0 / dtSeconds;
                    smoothedFps = smoothedFps == 0.0
                            ? instantFps
                            : smoothedFps * 0.9 + instantFps * 0.1;
                }
            }
            lastFrameNanos = now;
        } catch (Throwable ignored) { }

        // 2. Draw the overlay. Guard against null state during early load.
        Minecraft mcRef;
        try {
            mcRef = this.mc;
        } catch (Throwable t) {
            // Shadow field not resolvable — bail out.
            return;
        }
        if (mcRef == null) return;
        FontRenderer font;
        Entity player;
        try {
            font = mcRef.fontRenderer;
            player = mcRef.thePlayer;
        } catch (Throwable t) {
            return;
        }
        if (font == null || player == null) {
            return;
        }

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

        // Clock line
        if (evClient != null) {
            net.everlastingness.client.common.module.Module clockMod =
                evClient.module("clock_armor_hud");
            if (clockMod instanceof net.everlastingness.client.modules.hud.ClockArmorHudModule
                && clockMod.isEnabled()) {
                String time = ((net.everlastingness.client.modules.hud.ClockArmorHudModule) clockMod).getTimeString();
                y += font.FONT_HEIGHT + 1;
                font.drawStringWithShadow("Time: " + time, x, y, color);
            }
        }

        // Extended Lunar-parity HUD lines. Each is drawn only when its module
        // is registered and enabled.
        if (evClient != null) {
            // Ping: scan the net handler's playerInfoList for our entry
            // (1.7.10 API: NetHandlerPlayClient.playerInfoList of GuiPlayerInfo).
            if (isEnabled(evClient, "ping_display")) {
                int ping = 0;
                try {
                    java.util.List infos = mcRef.getNetHandler().playerInfoList;
                    String myName = mcRef.thePlayer.getCommandSenderName();
                    if (infos != null) {
                        for (Object o : infos) {
                            net.minecraft.client.gui.GuiPlayerInfo gpi =
                                (net.minecraft.client.gui.GuiPlayerInfo) o;
                            if (myName.equalsIgnoreCase(gpi.name)) {
                                ping = gpi.responseTime;
                                break;
                            }
                        }
                    }
                } catch (Throwable ignored) { }
                y += font.FONT_HEIGHT + 1;
                font.drawStringWithShadow("Ping: " + ping + "ms", x, y, color);
            }
            // Direction: from yaw. -180..180 -> 8-way compass.
            if (isEnabled(evClient, "direction_hud")) {
                float yaw = player.rotationYaw;
                String[] dirs = {"S","SW","W","NW","N","NE","E","SE"};
                int idx = Math.round((yaw % 360f + 360f) / 45f) % 8;
                y += font.FONT_HEIGHT + 1;
                font.drawStringWithShadow("Dir: " + dirs[idx] + " (" + Math.round(yaw) + ")", x, y, color);
            }
            // Server address.
            if (isEnabled(evClient, "server_address")) {
                String addr = "Singleplayer";
                try { if (mcRef.func_147104_D() != null) addr = mcRef.func_147104_D().serverIP; } catch (Throwable ignored) { }
                y += font.FONT_HEIGHT + 1;
                font.drawStringWithShadow("Server: " + addr, x, y, color);
            }
            // Memory usage.
            if (isEnabled(evClient, "memory_usage")) {
                Runtime rt = Runtime.getRuntime();
                long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
                long max = rt.maxMemory() / (1024 * 1024);
                y += font.FONT_HEIGHT + 1;
                font.drawStringWithShadow("Mem: " + used + "/" + max + "MB", x, y, color);
            }
            // Playtime.
            net.everlastingness.client.common.module.Module pt =
                evClient.module("playtime");
            if (pt instanceof net.everlastingness.client.modules.hud.PlaytimeModule && pt.isEnabled()) {
                y += font.FONT_HEIGHT + 1;
                font.drawStringWithShadow("Play: " + ((net.everlastingness.client.modules.hud.PlaytimeModule) pt).getElapsed(), x, y, color);
            }
            // Reach display.
            net.everlastingness.client.common.module.Module rd =
                evClient.module("reach_display");
            if (rd instanceof net.everlastingness.client.modules.combat.ReachDisplayModule && rd.isEnabled()) {
                String last = ((net.everlastingness.client.modules.combat.ReachDisplayModule) rd).getLastReach();
                if (!last.isEmpty()) {
                    y += font.FONT_HEIGHT + 1;
                    font.drawStringWithShadow("Reach: " + last, x, y, color);
                }
            }
            // Combo counter.
            net.everlastingness.client.common.module.Module cb =
                evClient.module("combo_counter");
            if (cb instanceof net.everlastingness.client.modules.combat.ComboCounterModule && cb.isEnabled()) {
                y += font.FONT_HEIGHT + 1;
                font.drawStringWithShadow("Combo: " + ((net.everlastingness.client.modules.combat.ComboCounterModule) cb).getCombo(), x, y, color);
            }
            // Armor status: per-piece durability of equipped armor + held item.
            if (isEnabled(evClient, "armor_status")) {
                try {
                    net.minecraft.item.ItemStack[] armor = mcRef.thePlayer.inventory.armorInventory;
                    for (int i = 3; i >= 0; i--) {
                        net.minecraft.item.ItemStack piece = armor[i];
                        if (piece != null) {
                            String label = piece.getDisplayName().length() > 18
                                    ? piece.getDisplayName().substring(0, 18)
                                    : piece.getDisplayName();
                            y += font.FONT_HEIGHT + 1;
                            font.drawStringWithShadow(label + " " + (piece.getMaxDamage() - piece.getItemDamageForDisplay()), x, y, color);
                        }
                    }
                } catch (Throwable ignored) { }
            }
            // Potion effects: name + remaining seconds.
            if (isEnabled(evClient, "potion_effects")) {
                try {
                    java.util.Collection effects = mcRef.thePlayer.getActivePotionEffects();
                    if (effects != null) {
                        for (Object o : effects) {
                            net.minecraft.potion.PotionEffect pe = (net.minecraft.potion.PotionEffect) o;
                            y += font.FONT_HEIGHT + 1;
                            font.drawStringWithShadow("Potion " + pe.getPotionID() + ": " + (pe.getDuration() / 20 + 1) + "s", x, y, color);
                        }
                    }
                } catch (Throwable ignored) { }
            }
            // Waila: name of the targeted block/entity, centered above hotbar.
            if (isEnabled(evClient, "waila")) {
                try {
                    net.minecraft.util.MovingObjectPosition mop = mcRef.objectMouseOver;
                    String looking = null;
                    if (mop != null && mop.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) {
                        looking = mcRef.theWorld.getBlock(mop.blockX, mop.blockY, mop.blockZ)
                                .getLocalizedName();
                    } else if (mop != null && mop.entityHit != null) {
                        looking = mop.entityHit.getCommandSenderName();
                    }
                    if (looking != null) {
                        int tw = font.getStringWidth(looking);
                        font.drawStringWithShadow(looking, 427 - tw / 2, 220, color);
                    }
                } catch (Throwable ignored) { }
            }
        }

        // Damage tint: full-screen red flash for ~800ms after taking damage.
        try {
            EverlastingnessClient evc2 = EverlastingnessClient.get();
            if (evc2 != null && isEnabled(evc2, "damage_tint")) {
                long since = System.nanoTime() - net.everlastingness.client.v1_7_10.mixin.combat.DamageTintState.getLastHurtNanos();
                if (since > 0 && since < 800_000_000L) {
                    float alpha = 0.45f * (1.0f - since / 800_000_000f);
                    int a = (int) (alpha * 255.0f) << 24;
                    int w = mcRef.currentScreen == null ? 854 : 854;
                    net.minecraft.client.gui.Gui.drawRect(0, 0, w, 480, a | 0x00FF0000);
                }
            }
        } catch (Throwable ignored) { }

        // Keystrokes: WASD/space key boxes, drawn on the right side of the HUD.
        // Each key is a 22x22 box (W/A/S/D) plus a wide space bar; pressed keys
        // are filled white with black text, unpressed outlined with white text,
        // mirroring Lunar's Keystrokes widget layout.
        if (evClient != null && isEnabled(evClient, "keystrokes")) {
            try {
                org.lwjgl.input.Keyboard.poll();
                int baseX = mcRef.currentScreen == null
                        ? mcRef.displayWidth / 4 - 70 : 70;
                int baseY = 60;
                int size = 22;
                drawKey(mcRef, font, baseX + size, baseY, size, size, "W",
                        org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_W));
                drawKey(mcRef, font, baseX, baseY + size, size, size, "A",
                        org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_A));
                drawKey(mcRef, font, baseX + size, baseY + size, size, size, "S",
                        org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_S));
                drawKey(mcRef, font, baseX + size * 2, baseY + size, size, size, "D",
                        org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_D));
                drawKey(mcRef, font, baseX, baseY + size * 2, size * 3, size, "————",
                        org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_SPACE));
            } catch (Throwable ignored) { }
        }
    }

    /** Draw a single keystroke box: white fill when pressed, outline otherwise. */
    private static void drawKey(Minecraft mc, FontRenderer font,
            int px, int py, int w, int h, String label, boolean pressed) {
        // Vanilla Gui.drawRect (func_73734_a) draws a flat colored quad.
        if (pressed) {
            net.minecraft.client.gui.Gui.drawRect(px, py, px + w, py + h, 0x80FFFFFF);
        } else {
            net.minecraft.client.gui.Gui.drawRect(px, py, px + w, py + h, 0x40000000);
        }
        int tx = px + (w - font.getStringWidth(label)) / 2;
        int ty = py + (h - font.FONT_HEIGHT) / 2;
        font.drawStringWithShadow(label, tx, ty, 0xFFFFFFFF);
    }

    private static boolean isEnabled(EverlastingnessClient client, String id) {
        net.everlastingness.client.common.module.Module m = client.module(id);
        return m != null && m.isEnabled();
    }
}
