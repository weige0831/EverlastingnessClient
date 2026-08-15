package net.everlastingness.client.v26_x.mixin.fog;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRendererFog {
    @Inject(method = "renderFog", at = @At("HEAD"), require = 0, cancellable = true)
    private void everlastingness$noFog(CallbackInfo ci) {
        try {
            EverlastingnessClient c = EverlastingnessClient.get();
            if (c == null) return;
            var m = c.module("fog");
            if (m != null && m.isEnabled()) {
                // Push fog to the far plane regardless of the shader-fog API era.
                try {
                    Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
                    rs.getMethod("setShaderFogStart", float.class).invoke(null, Float.MAX_VALUE);
                    rs.getMethod("setShaderFogEnd", float.class).invoke(null, Float.MAX_VALUE);
                } catch (Throwable ignored) {
                    GL11.glFogf(GL11.GL_FOG_START, Float.MAX_VALUE);
                    GL11.glFogf(GL11.GL_FOG_END, Float.MAX_VALUE);
                }
            }
        } catch (Throwable ignored) { }
    }
}
