package net.everlastingness.client.v1_20_x.mixin.chat;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatHud.class)
public class MixinChatHudTimestamps {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;)V",
            at = @At("HEAD"), ordinal = 0)
    private Text everlastingness$addTimestamp(Text message) {
        try {
            EverlastingnessClient c = EverlastingnessClient.get();
            if (c == null || message == null) return message;
            var m = c.module("chat_timestamps");
            if (m == null || !m.isEnabled()) return message;
            boolean h24 = ((net.everlastingness.client.modules.utility.ChatTimestampsModule) m).is24h();
            java.time.LocalTime now = java.time.LocalTime.now();
            String stamp = h24
                    ? now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    : now.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"));
            net.minecraft.text.Text prefixT;
            try {
                prefixT = (net.minecraft.text.Text) Text.class
                        .getMethod("literal", String.class).invoke(null, "[" + stamp + "] ");
            } catch (Throwable e) {
                prefixT = (net.minecraft.text.Text) Class
                        .forName("net.minecraft.text.LiteralText")
                        .getConstructor(String.class).newInstance("[" + stamp + "] ");
            }
            return (net.minecraft.text.Text) prefixT.getClass().getMethod("append", net.minecraft.text.Text.class).invoke(prefixT, message);
        } catch (Throwable t) {
            return message;
        }
    }
}
