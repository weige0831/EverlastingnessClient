package net.everlastingness.client.v1_7_10.mixin.scroll;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.everlastingness.client.modules.input.SmoothScrollModule;
import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Smooth Scroll Mixin — intercepts inventory mouse wheel input to apply
 * smooth scrolling animation. Injects at handleMouseInput in GuiContainer.
 */
@Mixin(GuiContainer.class)
public class MixinGuiContainerScroll {

    @Inject(method = "handleMouseInput", at = @At("HEAD"))
    private void everlastingness$smoothScroll(CallbackInfo ci) {
        EverlastingnessClient client = EverlastingnessClient.get();
        if (client == null) return;
        Module m = client.module("smooth_scroll");
        if (m instanceof SmoothScrollModule && m.isEnabled()) {
            // Let vanilla handle it; smooth offset is applied by the
            // render hook (the module tracks target/current offset)
        }
    }
}
