package net.everlastingness.client.v1_20_x.mixin.nickhider;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClientNick {
    @Inject(method = "tick", at = @At("HEAD"))
    private void everlastingness$hideTabList(CallbackInfo ci) {
        try {
            EverlastingnessClient c = EverlastingnessClient.get();
            if (c == null) return;
            var m = c.module("nick_hider");
            if (!(m instanceof net.everlastingness.client.modules.visual.NickHiderModule) || !m.isEnabled()) return;
            if (((net.everlastingness.client.modules.visual.NickHiderModule) m).isHidingOthers()) {
                Object opts = MinecraftClient.getInstance().options;
                Object key = null;
                for (String name : new String[] { "playerListKey", "keyPlayerList" }) {
                    try {
                        key = opts.getClass().getField(name).get(opts);
                        break;
                    } catch (Throwable ignored) { }
                }
                if (key != null) {
                    key.getClass().getMethod("setPressed", boolean.class).invoke(key, false);
                }
            }
        } catch (Throwable ignored) { }
    }
}
