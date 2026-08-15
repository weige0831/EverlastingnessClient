package net.everlastingness.client.v26_x.mixin.nickhider;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraftNick {
    @Inject(method = "tick", at = @At("HEAD"))
    private void everlastingness$hideTabList(CallbackInfo ci) {
        try {
            EverlastingnessClient c = EverlastingnessClient.get();
            if (c == null) return;
            var m = c.module("nick_hider");
            if (!(m instanceof net.everlastingness.client.modules.visual.NickHiderModule) || !m.isEnabled()) return;
            if (((net.everlastingness.client.modules.visual.NickHiderModule) m).isHidingOthers()) {
                Object opts = Minecraft.getInstance().options;
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
