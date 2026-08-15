package net.everlastingness.client.v26_x.mixin.weather;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class MixinLevelWeather {
    @Inject(method = "getRainLevel(F)F", at = @At("HEAD"), cancellable = true)
    private void everlastingness$overrideRain(float delta, CallbackInfoReturnable<Float> cir) {
        try {
            EverlastingnessClient c = EverlastingnessClient.get();
            if (c == null) return;
            var m = c.module("weather_changer");
            if (m instanceof net.everlastingness.client.modules.visual.WeatherChangerModule && m.isEnabled()) {
                switch (((net.everlastingness.client.modules.visual.WeatherChangerModule) m).getMode()) {
                    case CLEAR: cir.setReturnValue(0.0f); break;
                    case RAIN: cir.setReturnValue(1.0f); break;
                    default: break;
                }
            }
        } catch (Throwable ignored) { }
    }
}
