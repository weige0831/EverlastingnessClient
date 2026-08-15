package net.everlastingness.client.v1_20_x.mixin.cps;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClientCps {
    @Inject(remap = false, method = "doAttack()V", at = @At("HEAD"))
    private void everlastingness$left(CallbackInfo ci) { CpsState.onLeftClick(); }
    @Inject(remap = false, method = "doUse()V", at = @At("HEAD"))
    private void everlastingness$right(CallbackInfo ci) { CpsState.onRightClick(); }
}
