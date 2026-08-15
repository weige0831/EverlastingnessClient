package net.everlastingness.client.v1_7_10.mixin.nickhider;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Nick Hider Mixin — when hide-others is on, releases the tab-list key every
 * tick so the player list (and every name in it) is never rendered, mirroring
 * Lunar's NickHider tab-list hiding.
 *
 * <p>Target: Minecraft.runTick = func_71407_l. The player list only renders
 * while keyBindPlayerList reports pressed; un-pressing it suppresses the list
 * without touching any render method.</p>
 */
@Mixin(Minecraft.class)
public class MixinIngameNick {

    @Inject(remap = false, method = "func_71407_l()V", at = @At("HEAD"))
    private void everlastingness$hideTabList(CallbackInfo ci) {
        try {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client == null) return;
            Module m = client.module("nick_hider");
            if (!(m instanceof net.everlastingness.client.modules.visual.NickHiderModule)
                    || !m.isEnabled()) return;
            if (((net.everlastingness.client.modules.visual.NickHiderModule) m).isHidingOthers()) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null && mc.gameSettings != null && mc.gameSettings.keyBindPlayerList != null) {
                    mc.gameSettings.keyBindPlayerList.setKeyBindState(
                            mc.gameSettings.keyBindPlayerList.getKeyCode(), false);
                }
            }
        } catch (Throwable ignored) { }
    }
}
