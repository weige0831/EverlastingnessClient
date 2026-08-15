package net.everlastingness.client.v1_7_10.mixin.combat;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Damage detection Mixin — hooks EntityLivingBase.attackEntityFrom to:
 * <ul>
 *   <li>reset the Combo Counter when the local player takes damage;</li>
 *   <li>flag the Damage Tint module so the HUD overlay can flash red.</li>
 * </ul>
 *
 * <p>Target: EntityLivingBase.attackEntityFrom = func_70097_a. We filter to
 * the client player only (attacked entity == mc.thePlayer).</p>
 */
@Mixin(EntityLivingBase.class)
public class MixinEntityLivingBaseDamage {

    @Inject(remap = false,
            method = "func_70097_a(Lnet/minecraft/util/DamageSource;F)Z",
            at = @At("HEAD"))
    private void everlastingness$onDamage(DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        try {
            EntityLivingBase self = (EntityLivingBase) (Object) this;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || self != mc.thePlayer) return;

            EverlastingnessClient client = EverlastingnessClient.get();
            if (client == null) return;

            Module cb = client.module("combo_counter");
            if (cb instanceof net.everlastingness.client.modules.combat.ComboCounterModule
                    && cb.isEnabled()) {
                ((net.everlastingness.client.modules.combat.ComboCounterModule) cb).onHurt();
            }

            if (client.module("damage_tint") != null
                    && client.module("damage_tint").isEnabled()) {
                DamageTintState.markHurt();
            }
        } catch (Throwable ignored) { }
    }
}
