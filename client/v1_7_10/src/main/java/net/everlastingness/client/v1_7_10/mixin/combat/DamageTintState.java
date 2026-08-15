package net.everlastingness.client.v1_7_10.mixin.combat;

/**
 * Plain (non-mixin) holder for the Damage Tint flash timestamp. Mixin classes
 * may not expose non-private static members, so cross-mixin state lives here.
 */
public final class DamageTintState {
    private static volatile long lastHurtNanos = 0L;

    private DamageTintState() { }

    public static void markHurt() {
        lastHurtNanos = System.nanoTime();
    }

    public static long getLastHurtNanos() {
        return lastHurtNanos;
    }
}
