package net.everlastingness.client.v1_20_x.mixin.combat;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinInteractionManagerCombat {
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void everlastingness$onAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        try {
            EverlastingnessClient c = EverlastingnessClient.get();
            if (c == null || player == null || target == null) return;
            var rd = c.module("reach_display");
            if (rd instanceof net.everlastingness.client.modules.combat.ReachDisplayModule && rd.isEnabled()) {
                double dx = pos(player) - pos(target);
                double dy = pos(player, 1) - pos(target, 1);
                double dz = pos(player, 2) - pos(target, 2);
                ((net.everlastingness.client.modules.combat.ReachDisplayModule) rd)
                        .recordHit(Math.sqrt(dx * dx + dy * dy + dz * dz));
            }
            var cb = c.module("combo_counter");
            if (cb instanceof net.everlastingness.client.modules.combat.ComboCounterModule && cb.isEnabled()) {
                ((net.everlastingness.client.modules.combat.ComboCounterModule) cb).onHit();
            }
        } catch (Throwable ignored) { }
    }
    /** Entity position accessor: getX()/getZ()/getEyeY() modern, x/y/z fields legacy. */
    private static double pos(Object e) { return pos(e, 0); }
    private static double pos(Object e, int axis) {
        try {
            switch (axis) {
                case 1: return (Double) e.getClass().getMethod("getEyeY").invoke(e);
                case 2: return (Double) e.getClass().getMethod("getZ").invoke(e);
                default: return (Double) e.getClass().getMethod("getX").invoke(e);
            }
        } catch (Throwable t) {
            try {
                java.lang.reflect.Field f = e.getClass().getField(
                        axis == 1 ? "y" : axis == 2 ? "z" : "x");
                Object v = f.get(e);
                return v instanceof Double ? (Double) v : ((Float) v).doubleValue();
            } catch (Throwable ignored) {
                return 0.0;
            }
        }
    }
}
