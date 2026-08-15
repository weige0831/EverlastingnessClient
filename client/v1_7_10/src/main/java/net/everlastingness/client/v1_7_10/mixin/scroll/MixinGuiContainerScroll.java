package net.everlastingness.client.v1_7_10.mixin.scroll;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.everlastingness.client.modules.input.SmoothScrollModule;
import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Smooth Scroll Mixin &mdash; intercepts inventory mouse wheel input to apply
 * smooth scrolling animation.
 *
 * <p>Injects at {@code handleMouseInput} in {@link GuiScreen} (the declaring
 * class). We target GuiScreen rather than GuiContainer because GuiContainer
 * does not override {@code handleMouseInput} in 1.7.10 (it inherits the method
 * from GuiScreen), so the @Mixin target must be the declaring class for Mixin's
 * target validation to find the method. The injection still fires when
 * GuiContainer instances process mouse input, since they call the inherited
 * method.</p>
 */
@Mixin(GuiScreen.class)
public class MixinGuiContainerScroll {

    @Inject(remap = false, method = "func_146274_d()V", at = @At("HEAD"))
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
