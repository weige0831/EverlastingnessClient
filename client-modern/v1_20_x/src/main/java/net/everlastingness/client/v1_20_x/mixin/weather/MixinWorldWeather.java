package net.everlastingness.client.v1_20_x.mixin.weather;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class MixinWorldWeather {
    @Inject(method = "getRainGradient(F)F", at = @At("HEAD"), cancellable = true)
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
