package net.everlastingness.client.v26_x.mixin.chat;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public class MixinChatComponentTimestamps {
    @ModifyVariable(method = "addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"), ordinal = 0)
    private net.minecraft.network.chat.Component everlastingness$addTimestamp(net.minecraft.network.chat.Component message) {
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
            net.minecraft.network.chat.Component prefixT;
            try {
                prefixT = (net.minecraft.network.chat.Component) net.minecraft.network.chat.Component.class
                        .getMethod("literal", String.class).invoke(null, "[" + stamp + "] ");
            } catch (Throwable e) {
                prefixT = (net.minecraft.network.chat.Component) Class
                        .forName("net.minecraft.network.chat.Component")
                        .getConstructor(String.class).newInstance("[" + stamp + "] ");
            }
            return (net.minecraft.network.chat.Component) prefixT.getClass().getMethod("append", net.minecraft.network.chat.Component.class).invoke(prefixT, message);
        } catch (Throwable t) {
            return message;
        }
    }
}
