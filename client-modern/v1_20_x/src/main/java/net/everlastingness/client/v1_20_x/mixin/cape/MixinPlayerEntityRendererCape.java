package net.everlastingness.client.v1_20_x.mixin.cape;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class MixinPlayerEntityRendererCape {
    @Inject(remap = false, method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;DDDFF)V", at = @At("RETURN"))
    private void everlastingness$renderCape(AbstractClientPlayerEntity player, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
        try {
            // Custom cape render: would draw a GL quad textured with the
            // Everlastingness cape. Marker injection for now.
        } catch (Throwable ignored) { }
    }
}
