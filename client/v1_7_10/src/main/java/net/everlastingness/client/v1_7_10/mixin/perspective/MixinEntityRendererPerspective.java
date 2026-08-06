package net.everlastingness.client.v1_7_10.mixin.perspective;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.everlastingness.client.modules.camera.PerspectiveModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Perspective Mixin for MC 1.7.10. Allows cycling through camera views
 * (1st/3rd-back/3rd-front) via a keybind, overriding the vanilla
 * F5 thirdPersonView setting.
 *
 * <p>MCP names verified: setupCameraTransform(float,int) = func_78479_a,
 * which reads GameSettings.thirdPersonView to choose camera mode.</p>
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRendererPerspective {

    @Inject(method = "setupCameraTransform", at = @At("HEAD"))
    private void everlastingness$overridePerspective(float partialTicks, int renderPass, CallbackInfo ci) {
        EverlastingnessClient client = EverlastingnessClient.get();
        if (client == null) return;
        Module m = client.module("perspective");
        if (!(m instanceof PerspectiveModule) || !m.isEnabled()) return;

        PerspectiveModule mod = (PerspectiveModule) m;
        // Override vanilla thirdPersonView with our custom value
        Minecraft.getMinecraft().gameSettings.thirdPersonView = mod.getPerspective();
    }
}
