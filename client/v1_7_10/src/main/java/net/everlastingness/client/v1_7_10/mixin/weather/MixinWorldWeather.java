package net.everlastingness.client.v1_7_10.mixin.weather;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Weather Changer Mixin — forces rain strength to 0 (CLEAR) or 1 (RAIN)
 * regardless of server weather. The sky renderer, sounds and particle
 * systems all read getRainStrength/getThunderStrength, so overriding both
 * changes the full client-side weather presentation.
 */
@Mixin(World.class)
public class MixinWorldWeather {

    @Inject(remap = false, method = "func_72867_j(F)F", at = @At("HEAD"), cancellable = true)
    private void everlastingness$overrideRain(float partialTicks, CallbackInfoReturnable<Float> cir) {
        try {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client == null) return;
            Module m = client.module("weather_changer");
            if (!(m instanceof net.everlastingness.client.modules.visual.WeatherChangerModule)
                    || !m.isEnabled()) return;
            switch (((net.everlastingness.client.modules.visual.WeatherChangerModule) m).getMode()) {
                case CLEAR: cir.setReturnValue(0.0f); break;
                case RAIN: cir.setReturnValue(1.0f); break;
                default: /* SERVER — keep vanilla */ break;
            }
        } catch (Throwable ignored) { }
    }
}
