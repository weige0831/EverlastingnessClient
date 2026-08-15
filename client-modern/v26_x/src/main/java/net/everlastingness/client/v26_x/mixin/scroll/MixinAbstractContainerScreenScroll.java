package net.everlastingness.client.v26_x.mixin.scroll;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractContainerScreen.class)
public class MixinAbstractContainerScreenScroll {
    private static double smooth = 0, target = 0;
    @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"))
    private void everlastingness$onScroll(double mx, double my, double h, double v, CallbackInfo ci) {
        try { target += v; smooth += (target-smooth)*0.2; } catch (Throwable ignored) {}
    }
}
