package net.everlastingness.client.v1_7_10.mixin;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.event.RenderTickEvent;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Example Mixin: hooks Minecraft 1.7.10's per-frame render path to post a
 * {@link RenderTickEvent} onto the Everlastingness event bus. This is the
 * version-specific bridge that lets the version-agnostic HUD module draw its
 * overlay.
 *
 * <p>The target method {@code updateCameraAndRender(float)} is the 1.7.10
 * MCP-mapped name of the main render entry; RFG's deobf environment exposes it
 * under this readable name at compile time, then reobfuscates back to the
 * obfuscated name for the production jar.</p>
 *
 * <p>Phase 0 note: the exact target descriptor is verified at Phase 1 against
 * a real 1.7.10 dev environment. If RFG resolves the dev workspace, this mixin
 * compiles; otherwise the build will flag the missing symbol, which is the
 * signal to adjust the target.</p>
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    /**
     * Inject at the HEAD of the render method to fire the render-tick event
     * before Minecraft draws its own frame, giving modules a chance to prepare.
     */
    @Inject(method = "updateCameraAndRender", at = @At("HEAD"))
    private void everlastingness$postRenderTick(float partialTicks, CallbackInfo ci) {
        EverlastingnessClient client = EverlastingnessClient.get();
        if (client != null) {
            client.events().post(new RenderTickEvent(partialTicks));
        }
    }
}
