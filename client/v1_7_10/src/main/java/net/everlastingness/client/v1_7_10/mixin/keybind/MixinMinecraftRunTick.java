package net.everlastingness.client.v1_7_10.mixin.keybind;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.keybind.KeybindManager;
import net.everlastingness.client.common.module.Module;
import net.everlastingness.client.modules.camera.PerspectiveModule;
import net.everlastingness.client.modules.visual.ZoomModule;
import net.minecraft.client.Minecraft;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wires the {@link KeybindManager} into Minecraft 1.7.10's per-tick loop.
 *
 * <p>Injects at the HEAD of {@code Minecraft.runTick()} (SRG {@code func_71407_l})
 * and:</p>
 * <ol>
 *   <li>on the very first tick, sets the manager's key-state provider to LWJGL
 *       {@link Keyboard#isKeyDown(int)} and registers all feature keybinds
 *       (zoom hold on C, perspective cycle on G, coord-copy on X); and</li>
 *   <li>every tick, calls {@link KeybindManager#poll()} so registered keybinds
 *       fire their callbacks on the rising edge.</li>
 * </ol>
 *
 * <p>This is the per-version bridge: {@code Keyboard.isKeyDown} is non-event-
 * consuming, so polling here does not steal events from Minecraft's own input
 * processing (which reads {@code Keyboard.next()}).</p>
 */
@Mixin(Minecraft.class)
public class MixinMinecraftRunTick {

    private static boolean keybindsWired = false;
    /** Tracks whether the zoom key is currently held (edge detection). */
    private static boolean zoomHeld = false;

    @Inject(remap = false, method = "func_71407_l()V", at = @At("HEAD"))
    private void everlastingness$pollKeybinds(CallbackInfo ci) {
        if (!keybindsWired) {
            KeybindManager.get().setStateProvider(Keyboard::isKeyDown);
            registerKeybinds();
            keybindsWired = true;
        }
        pollZoomHold();
        KeybindManager.get().poll();
    }

    /**
     * Register the feature keybinds. LWJGL key codes from org.lwjgl.input.Keyboard:
     * KEY_C=46, KEY_G=42, KEY_X=45, KEY_R=19, KEY_F5=63, KEY_H=35.
     */
    private static void registerKeybinds() {
        EverlastingnessClient client = EverlastingnessClient.get();
        if (client == null) return;

        // Zoom is a HOLD keybind (active while C is down). We poll it directly
        // in pollZoomHold() rather than via the rising-edge KeybindManager.
        // Perspective cycle: G cycles 0->1->2->0 (front/third-back/third-front).
        KeybindManager.get().register("perspective_cycle", 42 /*KEY_G*/, action -> {
            Module m = client.module("perspective");
            if (m instanceof PerspectiveModule) {
                PerspectiveModule pm = (PerspectiveModule) m;
                int cur = pm.getPerspective();
                int next = (cur + 1) % 3;
                pm.setPerspective(next);
                System.out.println("[Everlastingness] Perspective set to " + next);
            }
        }, "Cycle camera perspective");

        // Coordinate copy: X copies current XYZ to the system clipboard.
        KeybindManager.get().register("coord_copy", 45 /*KEY_X*/, action -> {
            Module m = client.module("coord_copy");
            if (m != null && m.isEnabled() && Minecraft.getMinecraft().thePlayer != null) {
                try {
                    String coords = String.format("XYZ: %.1f %.1f %.1f",
                        Minecraft.getMinecraft().thePlayer.posX,
                        Minecraft.getMinecraft().thePlayer.posY,
                        Minecraft.getMinecraft().thePlayer.posZ);
                    java.awt.Toolkit.getDefaultToolkit()
                        .getSystemClipboard().setContents(
                            new java.awt.datatransfer.StringSelection(coords), null);
                    System.out.println("[Everlastingness] Copied: " + coords);
                } catch (Throwable t) {
                    System.err.println("[Everlastingness] coord copy failed: " + t);
                }
            }
        }, "Copy coordinates to clipboard");

        System.out.println("[Everlastingness] Keybinds registered (G=perspective, X=coord-copy, C=zoom-hold)");
    }

    /**
     * Poll the zoom hold key (C) every tick and update the ZoomModule's active
     * state. This is a hold-style keybind (active while held), unlike the
     * rising-edge toggles in KeybindManager.
     */
    private static void pollZoomHold() {
        EverlastingnessClient client = EverlastingnessClient.get();
        if (client == null) return;
        Module m = client.module("zoom");
        if (!(m instanceof ZoomModule)) return;
        ZoomModule zoom = (ZoomModule) m;
        if (!m.isEnabled()) return;
        boolean down;
        try {
            down = Keyboard.isKeyDown(46 /*KEY_C*/);
        } catch (Throwable t) {
            return;
        }
        if (down != zoomHeld) {
            zoomHeld = down;
            zoom.setZoomActive(down);
        }
    }
}

