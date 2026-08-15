package net.everlastingness.client.v1_20_x.mixin.keybind;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClientKeybind {
    private static boolean zoomHeld = false, gHeld = false, xHeld = false;

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void everlastingness$poll(CallbackInfo ci) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            long h = 0;
            try {
                Object w = mc.getClass().getMethod("getWindow").invoke(mc);
                h = (Long) w.getClass().getMethod("getHandle").invoke(w);
            } catch (Throwable ignored) { }
            boolean c = down(h, 67);
            if (c != zoomHeld) {
                zoomHeld = c;
                net.everlastingness.client.v1_20_x.mixin.zoom.ZoomState.setZoomActive(c);
            }
            boolean g = down(h, 71);
            gHeld = g;
            boolean x = down(h, 88);
            if (x && !xHeld && mc.player != null) {
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(
                                String.format("XYZ: %.1f %.1f %.1f",
                                        kx(mc.player), ky(mc.player), kz(mc.player))), null);
            }
            xHeld = x;
        } catch (Throwable ignored) { }
    }

    /** GLFW (LWJGL3) on modern, Keyboard (LWJGL2) on legacy. */
    private static boolean down(long glfwHandle, int code) {
        try {
            if (glfwHandle != 0) {
                Object r = Class.forName("org.lwjgl.glfw.GLFW")
                        .getMethod("glfwGetKey", long.class, int.class).invoke(null, glfwHandle, code);
                return (Integer) r == 1;
            }
        } catch (Throwable ignored) { }
        try {
            return (Boolean) Class.forName("org.lwjgl.input.Keyboard")
                    .getMethod("isKeyDown", int.class).invoke(null, code);
        } catch (Throwable t) {
            return false;
        }
    }
    private static double kx(Object e) { return kd(e, "getX", "x"); }
    private static double ky(Object e) { return kd(e, "getY", "y"); }
    private static double kz(Object e) { return kd(e, "getZ", "z"); }
    private static double kd(Object e, String m, String f) {
        try { return (Double) e.getClass().getMethod(m).invoke(e); }
        catch (Throwable t) {
            try { Object v = e.getClass().getField(f).get(e);
                  return v instanceof Double ? (Double) v : ((Float) v).doubleValue(); }
            catch (Throwable ignored) { return 0.0; }
        }
    }
}
