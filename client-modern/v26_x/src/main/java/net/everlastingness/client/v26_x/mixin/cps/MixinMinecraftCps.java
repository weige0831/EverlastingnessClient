package net.everlastingness.client.v26_x.mixin.cps;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraftCps {
    @Inject(remap = false, method = "startAttack()Z", at = @At("HEAD"))
    private void everlastingness$left(CallbackInfo ci) { CpsState.onLeftClick(); }
    @Inject(remap = false, method = "startUseItem()V", at = @At("HEAD"))
    private void everlastingness$right(CallbackInfo ci) { CpsState.onRightClick(); }
}
