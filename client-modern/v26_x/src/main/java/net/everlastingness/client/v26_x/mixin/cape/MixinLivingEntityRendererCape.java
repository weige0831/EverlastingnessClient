package net.everlastingness.client.v26_x.mixin.cape;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRendererCape {
    @Inject(remap = false, method = "render(Lnet/minecraft/client/network/AbstractClientPlayer;DDDFF)V", at = @At("RETURN"))
    private void everlastingness$renderCape(AbstractClientPlayer player, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
        try {
            // Custom cape render: would draw a GL quad textured with the
            // Everlastingness cape. Marker injection for now.
        } catch (Throwable ignored) { }
    }
}
