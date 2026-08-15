package net.everlastingness.client.v26_x.mixin.autotext;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraftAutoText {
    private static boolean ggHeld = false, hubHeld = false;

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void everlastingness$autoText(CallbackInfo ci) {
        try {
            EverlastingnessClient c = EverlastingnessClient.get();
            if (c == null) return;
            var m = c.module("auto_text");
            if (m == null || !m.isEnabled()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) { ggHeld = false; hubHeld = false; return; }
            // GLFW on modern (LWJGL3), org.lwjgl.input.Keyboard on legacy (LWJGL2).
            Object windowObj = null; long handle = 0;
            try {
                windowObj = mc.getClass().getMethod("getWindow").invoke(mc);
                handle = (Long) windowObj.getClass().getMethod("getHandle").invoke(windowObj);
            } catch (Throwable ignored) { }
            boolean gg = isDown(handle, 73);  // I
            if (gg && !ggHeld) sendChat(mc, "gg");
            ggHeld = gg;
            boolean hub = isDown(handle, 79); // O
            if (hub && !hubHeld) sendChat(mc, "/hub");
            hubHeld = hub;
        } catch (Throwable ignored) { }
    }

    private static boolean isDown(long glfwHandle, int code) {
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

    private static void sendChat(Minecraft mc, String msg) {
        try {
            mc.player.getClass().getMethod("sendChatMessage", String.class).invoke(mc.player, msg);
        } catch (Throwable t) {
            try {
                var h = mc.player.getClass().getMethod("networkHandler").invoke(mc.player);
                h.getClass().getMethod("sendChatMessage", String.class).invoke(h, msg);
            } catch (Throwable ignored) { }
        }
    }
}
