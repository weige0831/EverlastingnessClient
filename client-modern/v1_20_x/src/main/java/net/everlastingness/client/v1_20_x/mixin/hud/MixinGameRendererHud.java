package net.everlastingness.client.v1_20_x.mixin.hud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public class MixinGameRendererHud {
    @Shadow private MinecraftClient client;
    private long lastFrameNanos = 0L; private double smoothedFps = 0.0; private long lastLog = 0L;
    @Inject(method = "render(FJZ)V", at = @At("HEAD"))
    private void everlastingness$drawHud(float td, long st, boolean t, CallbackInfo ci) {
        try { updateFps(); if (client==null||client.player==null) return;
            if (System.nanoTime()-lastLog>5e9) { lastLog=System.nanoTime(); Entity p=client.player;
                System.out.println("[Everlastingness] HUD XYZ: "+pos(p)+" FPS: "+(int)Math.round(smoothedFps)); }
        } catch (Throwable ignored) {}
    }
    private void updateFps() { long n=System.nanoTime(); if(lastFrameNanos!=0){double dt=(n-lastFrameNanos)/1e9;if(dt>0){double i=1/dt;smoothedFps=smoothedFps==0?i:smoothedFps*0.9+i*0.1;}} lastFrameNanos=n; }
    private static String pos(Object e) {
        try {
            double x = (Double) e.getClass().getMethod("getX").invoke(e);
            double y = (Double) e.getClass().getMethod("getY").invoke(e);
            double z = (Double) e.getClass().getMethod("getZ").invoke(e);
            return x + " " + y + " " + z;
        } catch (Throwable t) {
            try {
                Object x = e.getClass().getField("x").get(e);
                Object y = e.getClass().getField("y").get(e);
                Object z = e.getClass().getField("z").get(e);
                return x + " " + y + " " + z;
            } catch (Throwable ignored) {
                return "?";
            }
        }
    }
}
