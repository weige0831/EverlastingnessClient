package net.everlastingness.client.v1_7_10.mixin.fps;

import net.everlastingness.client.common.fps.EntityCullConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Entity render-distance culling — the core of Everlastingness's FPS
 * optimization module for Minecraft 1.7.10.
 *
 * <p>Injects at the HEAD of {@code RenderManager.renderEntitySimple(Entity,float)}
 * (the per-entity render entry called from {@code RenderGlobal.renderEntities})
 * and <strong>cancels</strong> the render for any entity farther than the
 * configured cull distance from the camera view entity. Skipping the expensive
 * per-entity render (model binding, GL draws, lighting) for distant entities is
 * a real, measurable FPS win — it is the same class of optimisation Sodium /
 * Canvas / Lithium apply to entity rendering.</p>
 *
 * <p>Safety guarantees:</p>
 * <ul>
 *   <li>The local player ({@code mc.thePlayer}) and the view entity are never
 *       culled, so first-person rendering and camera logic stay intact.</li>
 *   <li>Only takes effect when {@link EntityCullConfig#isEnabled()} is true and
 *       a finite cull distance is set; otherwise falls through to vanilla.</li>
 *   <li>Distance is measured in squared units and compared to a squared
 *       threshold (no sqrt), keeping the check cheap per-entity.</li>
 * </ul>
 *
 * <p>MCP names verified against the decompiled 1.7.10 source:
 * {@code RenderManager.renderEntitySimple(Entity,float)} line 249 returns
 * boolean; {@code Entity.getDistanceSqToEntity(Entity)}; {@code Minecraft.renderViewEntity}.</p>
 */
@Mixin(RenderManager.class)
public class MixinEntityCull {

    @Inject(remap = false, 
        method = "func_147937_a(Lnet/minecraft/entity/Entity;F)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void everlastingness$cullDistantEntity(
            Entity entity, float partialTicks,
            CallbackInfoReturnable<Boolean> cir) {
        EntityCullConfig config = EntityCullConfig.get();
        if (!config.isEnabled() || entity == null) {
            return; // module off — vanilla rendering
        }

        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc != null ? mc.renderViewEntity : null;
        if (view == null) {
            return; // no camera reference yet — render normally
        }

        // Never cull the local player or the camera entity itself.
        if (entity == view || (mc.thePlayer != null && entity == mc.thePlayer)) {
            return;
        }

        double cullSq = config.getCullDistanceSq();
        if (cullSq <= 0.0) {
            return; // culling disabled (distance 0 = render everything)
        }

        double distSq;
        try {
            distSq = entity.getDistanceSqToEntity(view);
        } catch (Throwable ignored) {
            return; // bail out to vanilla on any distance-computation error
        }

        if (distSq > cullSq) {
            // Cancel the render and return the same type as the target (boolean).
            // renderEntitySimple's documented return is "whether anything was
            // rendered" — false correctly signals "not rendered".
            cir.setReturnValue(false);
        }
    }
}
