package net.everlastingness.client.v1_20_x.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Modern-era (1.20.x) Mixin bridge: hooks Minecraft's per-frame render method.
 *
 * <p>This Phase 2 build is intentionally self-contained (it does not yet depend
 * on the shared <c>:common</c> library, which lives in the separate legacy
 * <c>client/</c> Gradle build). It proves the Loom + Yarn + Mixin toolchain
 * compiles and remaps correctly for 1.20.x; the shared event-bus wiring is
 * layered on once the toolchain is verified.</p>
 *
 * <p>Target verified against the 1.20.x Yarn mappings: the per-frame render
 * method is {@code GameRenderer.render(float tickDelta, long startTime,
 * boolean tick)} — descriptor {@code render(FJZ)V}.</p>
 */
@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    private static long frameCount = 0;

    /**
     * Fire at the HEAD of each frame. Logs once every 300 frames as a
     * self-contained injection proof (visible in the game log).
     */
    @Inject(method = "render(FJZ)V", at = @At("HEAD"))
    private void everlastingness$postRenderTick(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        if (++frameCount % 300 == 1) {
            System.out.println("[Everlastingness] Mixin active on 1.20.x — frame " + frameCount
                    + " (tickDelta=" + tickDelta + ")");
        }
    }
}
