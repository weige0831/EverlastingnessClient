package net.everlastingness.client.v1_7_10.mixin.keybind;

import net.everlastingness.client.common.keybind.KeybindManager;
import net.minecraft.client.Minecraft;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wires the {@link KeybindManager} into Minecraft 1.7.10's per-tick loop.
 *
 * <p>Injects at the HEAD of {@code Minecraft.runTick()} (SRG {@code func_71407_l})
 * and:</p>
 * <ol>
 *   <li>on the very first tick, sets the manager's key-state provider to LWJGL
 *       {@link Keyboard#isKeyDown(int)}; and</li>
 *   <li>every tick, calls {@link KeybindManager#poll()} so registered keybinds
 *       fire their callbacks on the rising edge.</li>
 * </ol>
 *
 * <p>This is the per-version bridge: {@code Keyboard.isKeyDown} is non-event-
 * consuming, so polling here does not steal events from Minecraft's own input
 * processing (which reads {@code Keyboard.next()}). MCP name {@code runTick}
 * verified against the decompiled 1.7.10 source (line 1658,
 * {@code public void runTick()}).</p>
 */
@Mixin(Minecraft.class)
public class MixinMinecraftRunTick {

    private static boolean keybindsWired = false;

    @Inject(method = "runTick()V", at = @At("HEAD"))
    private void everlastingness$pollKeybinds(CallbackInfo ci) {
        if (!keybindsWired) {
            KeybindManager.get().setStateProvider(Keyboard::isKeyDown);
            keybindsWired = true;
        }
        KeybindManager.get().poll();
    }
}
