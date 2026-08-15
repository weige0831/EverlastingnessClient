package net.everlastingness.client.v1_7_10.mixin.hitbox;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hitbox Mixin — enables vanilla's entity debug bounding boxes without the
 * F3+B debug screen. RenderManager already renders wireframe hitboxes when
 * its static debugBoundingBox flag is set; we toggle the flag every frame
 * from the module state so hitboxes show only while the mod is on.
 *
 * <p>Target: RenderManager.renderEntitySimple = func_147937_a (the same
 * entry point the entity-cull fps mixin hooks).</p>
 */
@Mixin(RenderManager.class)
public class MixinRenderManagerHitbox {

    @Inject(remap = false, method = "func_147937_a(Lnet/minecraft/entity/Entity;F)Z",
            at = @At("HEAD"))
    private void everlastingness$showHitboxes(net.minecraft.entity.Entity entity, float partialTicks,
            CallbackInfoReturnable<Boolean> cir) {
        try {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client == null) return;
            Module m = client.module("hitbox");
            RenderManager.debugBoundingBox = m != null && m.isEnabled();
        } catch (Throwable ignored) { }
    }
}
