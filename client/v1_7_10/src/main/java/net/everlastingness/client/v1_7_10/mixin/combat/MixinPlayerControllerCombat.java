package net.everlastingness.client.v1_7_10.mixin.combat;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Combat detection Mixin — hooks the client's entity-attack call to drive
 * the Reach Display and Combo Counter modules:
 * <ul>
 *   <li>ReachDisplay: records the eye-to-hit-entity distance of the attack.</li>
 *   <li>ComboCounter: increments the combo on each attack (reset on hurt via
 *       the damage mixin).</li>
 * </ul>
 *
 * <p>Target: PlayerControllerMP.attackEntity = func_78764_a.</p>
 */
@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerCombat {

    @Inject(remap = false,
            method = "func_78764_a(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;)V",
            at = @At("HEAD"))
    private void everlastingness$onAttack(EntityPlayer player, Entity target, CallbackInfo ci) {
        try {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client == null || player == null) return;

            // Reach display.
            Module rd = client.module("reach_display");
            if (rd instanceof net.everlastingness.client.modules.combat.ReachDisplayModule
                    && rd.isEnabled() && target != null) {
                double dx = player.posX - target.posX;
                double dy = player.posY + player.getEyeHeight() - target.posY;
                double dz = player.posZ - target.posZ;
                ((net.everlastingness.client.modules.combat.ReachDisplayModule) rd)
                        .recordHit(Math.sqrt(dx * dx + dy * dy + dz * dz));
            }

            // Combo counter.
            Module cb = client.module("combo_counter");
            if (cb instanceof net.everlastingness.client.modules.combat.ComboCounterModule
                    && cb.isEnabled()) {
                ((net.everlastingness.client.modules.combat.ComboCounterModule) cb).onHit();
            }
        } catch (Throwable ignored) { }
    }
}
