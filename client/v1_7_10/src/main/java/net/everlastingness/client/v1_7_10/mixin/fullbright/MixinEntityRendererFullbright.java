package net.everlastingness.client.v1_7_10.mixin.fullbright;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.everlastingness.client.modules.visual.FullbrightModule;
import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fullbright Mixin for MC 1.7.10. Cancels the vanilla updateLightmap()
 * when Fullbright is enabled, keeping the lightmap at maximum brightness.
 *
 * <p>MCP name: updateLightmap(float) = func_78472_g, line 864.</p>
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRendererFullbright {

    @Inject(remap = false, method = "func_78472_g(F)V", at = @At("HEAD"), cancellable = true)
    private void everlastingness$fullbright(float partialTicks, CallbackInfo ci) {
        EverlastingnessClient client = EverlastingnessClient.get();
        if (client == null) return;
        Module m = client.module("fullbright");
        if (m != null && m.isEnabled()) {
            // Cancel vanilla lightmap update — keeps everything at full brightness
            ci.cancel();
        }
    }
}
