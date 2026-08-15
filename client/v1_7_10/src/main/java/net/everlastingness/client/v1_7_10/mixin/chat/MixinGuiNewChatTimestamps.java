package net.everlastingness.client.v1_7_10.mixin.chat;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Chat Timestamps Mixin — prefixes every incoming chat message with the
 * local wall-clock time in [HH:mm] form, mirroring Lunar's chat timestamps.
 *
 * <p>Target: GuiNewChat.printChatMessage(IChatComponent) = func_146227_a.
 * We modify the incoming message component at HEAD by wrapping it in a new
 * ChatComponentText starting with the timestamp.</p>
 */
@Mixin(GuiNewChat.class)
public class MixinGuiNewChatTimestamps {

    @ModifyVariable(remap = false, method = "func_146227_a(Lnet/minecraft/util/IChatComponent;)V",
            at = @At("HEAD"), ordinal = 0)
    private IChatComponent everlastingness$addTimestamp(IChatComponent message) {
        try {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client == null) return message;
            Module m = client.module("chat_timestamps");
            if (m == null || !m.isEnabled() || message == null) return message;
            java.time.LocalTime now = java.time.LocalTime.now();
            boolean h24 = ((net.everlastingness.client.modules.utility.ChatTimestampsModule) m).is24h();
            String stamp = h24
                    ? now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    : now.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"));
            ChatComponentText stamped = new ChatComponentText("[" + stamp + "] ");
            stamped.appendSibling(message);
            return stamped;
        } catch (Throwable t) {
            return message;
        }
    }
}
