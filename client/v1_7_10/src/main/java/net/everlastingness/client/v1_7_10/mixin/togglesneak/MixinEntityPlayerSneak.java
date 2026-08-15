package net.everlastingness.client.v1_7_10.mixin.togglesneak;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Toggle Sneak Mixin — makes the sneak key latching: pressing it once toggles
 * sneak on, pressing again toggles off, instead of hold-to-sneak.
 *
 * <p>Target: EntityPlayerSP.onLivingUpdate = func_70636_d, which reads
 * movementInput.sneak each tick. We maintain a toggle flag on the rising edge
 * of the sneak key and force movementInput.sneak accordingly.</p>
 */
@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSneak {

    private static boolean sneakToggled = false;
    private static boolean wasKeyDown = false;

    @Inject(remap = false, method = "func_70636_d()V", at = @At("HEAD"))
    private void everlastingness$toggleSneak(CallbackInfo ci) {
        try {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client == null) return;
            Module m = client.module("toggle_sneak");
            if (m == null || !m.isEnabled()) {
                sneakToggled = false;
                return;
            }
            EntityPlayerSP self = (EntityPlayerSP) (Object) this;
            boolean down = net.minecraft.client.Minecraft.getMinecraft().gameSettings.keyBindSneak.getIsKeyPressed();
            if (down && !wasKeyDown) {
                sneakToggled = !sneakToggled;
            }
            wasKeyDown = down;
            // Drive the movement input with the toggled state.
            if (self.movementInput != null) {
                self.movementInput.sneak = sneakToggled;
            }
        } catch (Throwable ignored) { }
    }
}
