package net.everlastingness.client.v1_7_10.mixin.zoom;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.everlastingness.client.modules.visual.ZoomModule;
import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Zoom Mixin — hooks getFOVModifier to apply smooth zoom.
 * MCP name: getFOVModifier(float, boolean) = func_78481_a
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRendererZoom {

    @Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true)
    private void everlastingness$zoom(float partialTicks, boolean useFOVSetting,
                                      CallbackInfoReturnable<Float> cir) {
        EverlastingnessClient client = EverlastingnessClient.get();
        if (client == null) return;
        Module m = client.module("zoom");
        if (m instanceof ZoomModule && m.isEnabled()) {
            float adjusted = ((ZoomModule) m).adjustFov(cir.getReturnValue(), partialTicks);
            cir.setReturnValue(adjusted);
        }
    }
}
