package net.everlastingness.client.v26_x.mixin.togglesneak;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "net.minecraft.client.player.LocalPlayer",
        "net.minecraft.client.player.LocalPlayer" }, remap = false)
public class MixinClientPlayerSneak {
    private static boolean toggle = false;
    private static boolean wasDown = false;

    @Inject(method = "aiStep", at = @At("HEAD"), require = 0, remap = false)
    private void everlastingness$toggleSneak(CallbackInfo ci) {
        try {
            EverlastingnessClient c = EverlastingnessClient.get();
            if (c == null) return;
            var m = c.module("toggle_sneak");
            if (m == null || !m.isEnabled()) { toggle = false; return; }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) { wasDown = false; return; }
            Object opts = mc.options;
            Object key = null;
            for (String name : new String[] { "sneakKey", "keySneak" }) {
                try {
                    key = opts.getClass().getField(name).get(opts);
                    break;
                } catch (Throwable ignored) { }
            }
            boolean down = false;
            if (key != null) {
                down = (Boolean) key.getClass().getMethod("isPressed").invoke(key);
            }
            if (down && !wasDown) toggle = !toggle;
            wasDown = down;
            Object self = this;
            self.getClass().getMethod("setSneaking", boolean.class).invoke(self, toggle);
        } catch (Throwable ignored) { }
    }
}
