package net.everlastingness.client.v1_7_10.mixin.cps;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.everlastingness.client.modules.input.CpsCounterModule;
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CPS counter Mixin for MC 1.7.10. Hooks the left-click handler
 * (func_147115_a) to count left clicks for the CPS counter module.
 */
@Mixin(Minecraft.class)
public class MixinMinecraftCps {

    @Inject(remap = false, method = "func_147115_a(Z)V", at = @At("HEAD"))
    private void everlastingness$onLeftClick(boolean leftClick, CallbackInfo ci) {
        if (!leftClick) return;
        EverlastingnessClient client = EverlastingnessClient.get();
        if (client == null) return;
        Module m = client.module("cps_counter");
        if (m instanceof CpsCounterModule) {
            ((CpsCounterModule) m).onLeftClick();
        }
    }
}
