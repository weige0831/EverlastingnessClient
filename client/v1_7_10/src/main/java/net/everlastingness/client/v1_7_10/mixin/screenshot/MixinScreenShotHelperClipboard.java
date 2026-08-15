package net.everlastingness.client.v1_7_10.mixin.screenshot;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ScreenShotHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.imageio.ImageIO;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.stream.FileImageOutputStream;

/**
 * Screenshot Viewer Mixin — after F2 saves a screenshot, copies the saved PNG
 * file to the system clipboard so it can be pasted instantly, mirroring
 * Lunar's Screenshot copy-to-clipboard.
 *
 * <p>Target: ScreenShotHelper.saveScreenshot = func_148260_a / func_148259_a.
 * The vanilla method writes the file and returns the chat component; we read
 * the file back after RETURN and put it on the clipboard.</p>
 */
@Mixin(ScreenShotHelper.class)
public class MixinScreenShotHelperClipboard {

    @Inject(remap = false,
            method = "func_148260_a(Ljava/io/File;IILnet/minecraft/client/shader/Framebuffer;)Lnet/minecraft/util/IChatComponent;",
            at = @At("RETURN"))
    private static void everlastingness$copyScreenshot(File gameDir, int width, int height,
            Framebuffer buffer, CallbackInfoReturnable<IChatComponent> cir) {
        copyLatest(gameDir);
    }

    @Inject(remap = false,
            method = "func_148259_a(Ljava/io/File;Ljava/lang/String;IILnet/minecraft/client/shader/Framebuffer;)Lnet/minecraft/util/IChatComponent;",
            at = @At("RETURN"))
    private static void everlastingness$copyScreenshot2(File gameDir, String name, int width, int height,
            Framebuffer buffer, CallbackInfoReturnable<IChatComponent> cir) {
        copyLatest(gameDir);
    }

    private static void copyLatest(File gameDir) {
        try {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client == null) return;
            Module m = client.module("screenshot_viewer");
            if (m == null || !m.isEnabled()) return;

            File dir = new File(gameDir, "screenshots");
            File[] files = dir.listFiles();
            if (files == null || files.length == 0) return;
            File latest = files[0];
            for (File f : files) {
                if (f.lastModified() > latest.lastModified()) latest = f;
            }
            // Read the image and put it on the clipboard as a Transferable.
            BufferedImage img = ImageIO.read(latest);
            if (img == null) return;
            java.awt.datatransfer.Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
            clip.setContents(new java.awt.datatransfer.Transferable() {
                @Override public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
                    return new java.awt.datatransfer.DataFlavor[] {
                            java.awt.datatransfer.DataFlavor.imageFlavor };
                }
                @Override public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor f) {
                    return java.awt.datatransfer.DataFlavor.imageFlavor.equals(f);
                }
                @Override public Object getTransferData(java.awt.datatransfer.DataFlavor f) {
                    return img;
                }
            }, null);
        } catch (Throwable ignored) { }
    }
}
