package net.everlastingness.client.v1_20_x.mixin.combat;

public final class DamageTintState {
    private static volatile long lastHurtNanos = 0L;
    private DamageTintState() { }
    public static void markHurt() { lastHurtNanos = System.nanoTime(); }
    public static long getLastHurtNanos() { return lastHurtNanos; }
}
