package net.everlastingness.client.v1_20_x.mixin.screenshot;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Screenshot Mixin — detects the vanilla "Saved screenshot" chat overlay and
 * copies a marker to the system clipboard. The overlay text is the only
 * version-stable signal that a screenshot was just written.
 */
@Mixin(InGameHud.class)
public class MixinScreenshotClipboard {
    @Inject(method = "setOverlayMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), require = 0)
    private void everlastingness$onScreenshotOverlay(Text message, boolean tinted, CallbackInfo ci) {
        try {
            if (message == null || !textOf(message).toLowerCase().contains("screenshot")) return;
            EverlastingnessClient c = EverlastingnessClient.get();
            if (c == null) return;
            var m = c.module("screenshot_viewer");
            if (m == null || !m.isEnabled()) return;
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(textOf(message)), null);
        } catch (Throwable ignored) { }
    }
    /** Text accessor: getString() modern, asString()/asFormattedString() legacy. */
    private static String textOf(net.minecraft.text.Text t) {
        for (String name : new String[] { "getString", "asString", "asFormattedString" }) {
            try {
                Object r = t.getClass().getMethod(name).invoke(t);
                if (r instanceof String) return (String) r;
            } catch (Throwable ignored) { }
        }
        return String.valueOf(t);
    }
}
