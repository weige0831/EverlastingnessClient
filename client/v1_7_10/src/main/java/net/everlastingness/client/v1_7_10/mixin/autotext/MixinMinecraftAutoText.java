package net.everlastingness.client.v1_7_10.mixin.autotext;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Auto Text Mixin — polls macro keybinds every client tick and sends the
 * bound command/message to chat on rising edge, mirroring Lunar's
 * AutoTextHotkey. Default macros: I = "gg" (auto-GG), O = "/hub".
 */
@Mixin(Minecraft.class)
public class MixinMinecraftAutoText {

    private static boolean ggHeld = false;
    private static boolean hubHeld = false;

    @Inject(remap = false, method = "func_71407_l()V", at = @At("HEAD"))
    private void everlastingness$autoText(CallbackInfo ci) {
        try {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client == null) return;
            Module m = client.module("auto_text");
            if (m == null || !m.isEnabled()) return;
            Minecraft mc = (Minecraft) (Object) this;
            if (mc.thePlayer == null) return;

            // Only trigger when no chat/inventory screen is open, so typing
            // the letter in chat doesn't fire the macro.
            if (mc.currentScreen != null) {
                ggHeld = false;
                hubHeld = false;
                return;
            }

            boolean gg = Keyboard.isKeyDown(Keyboard.KEY_I);
            if (gg && !ggHeld) {
                mc.thePlayer.sendChatMessage("gg");
            }
            ggHeld = gg;

            boolean hub = Keyboard.isKeyDown(Keyboard.KEY_O);
            if (hub && !hubHeld) {
                mc.thePlayer.sendChatMessage("/hub");
            }
            hubHeld = hub;
        } catch (Throwable ignored) { }
    }
}
