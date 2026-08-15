package net.everlastingness.client.v1_20_x.mixin.scroll;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(HandledScreen.class)
public class MixinHandledScreenScroll {
    private static double smooth = 0, target = 0;
    @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"))
    private void everlastingness$onScroll(double mx, double my, double h, double v, CallbackInfo ci) {
        try { target += v; smooth += (target-smooth)*0.2; } catch (Throwable ignored) {}
    }
}
