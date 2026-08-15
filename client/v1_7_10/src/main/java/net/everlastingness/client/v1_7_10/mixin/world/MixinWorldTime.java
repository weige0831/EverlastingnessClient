package net.everlastingness.client.v1_7_10.mixin.world;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Time Changer Mixin — overrides the world time used for celestial angle
 * computation (sun/moon position, sky color) so the client renders the
 * configured fixed time without touching the server's actual time.
 *
 * <p>Target: World.getCelestialAngle(float) = func_72826_c. The method
 * delegates to provider.calculateCelestialAngle(worldInfo.getWorldTime(), f);
 * we inject at HEAD and return the angle for our fixed time instead.</p>
 */
@Mixin(World.class)
public class MixinWorldTime {

    @Inject(remap = false, method = "func_72826_c(F)F", at = @At("HEAD"), cancellable = true)
    private void everlastingness$overrideTime(float partialTicks, CallbackInfoReturnable<Float> cir) {
        try {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client == null) return;
            Module m = client.module("time_changer");
            if (!(m instanceof net.everlastingness.client.modules.visual.TimeChangerModule)
                    || !m.isEnabled()) return;
            long fixed = ((net.everlastingness.client.modules.visual.TimeChangerModule) m).getFixedTime();
            World self = (World) (Object) this;
            // Same math as WorldProvider.calculateCelestialAngle but with the
            // client-fixed time. (0.0 = sunrise, 0.25 = noon, 0.5 = sunset.)
            float angle = ((float) fixed + partialTicks) / 24000.0f - 0.25f;
            if (angle < 0.0f) angle += 1.0f;
            cir.setReturnValue(angle);
        } catch (Throwable ignored) { }
    }
}
