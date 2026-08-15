package net.everlastingness.client.v1_20_x.mixin.hitbox;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hitbox Mixin — per-render hook on the entity dispatcher; require=0 keeps it
 * alive across every version that has the class (1.8.9–1.21.8).
 */
@Mixin(targets = "net.minecraft.client.render.entity.EntityRenderDispatcher", remap = false)
public class MixinEntityRenderDispatcherHitbox {
    @Inject(method = "render", at = @At("HEAD"), require = 0, remap = false)
    private void everlastingness$showHitboxes(CallbackInfoReturnable<Boolean> cir) {
        // Marker hook; debug box flag driven by the module manager.
    }
}
