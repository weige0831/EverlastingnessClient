package net.everlastingness.client.v1_20_x.mixin.cps;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * CPS state holder. Mixin classes cannot declare non-private static members
 * (InvalidMixinException at apply time), so the deque and counters live here.
 */
public final class CpsState {

    private static final ConcurrentLinkedDeque<Long> left = new ConcurrentLinkedDeque<>();
    private static final ConcurrentLinkedDeque<Long> right = new ConcurrentLinkedDeque<>();

    private CpsState() {
    }

    public static void onLeftClick() {
        left.add(System.currentTimeMillis());
    }

    public static void onRightClick() {
        right.add(System.currentTimeMillis());
    }

    public static int getLeftCps() {
        return clean(left);
    }

    public static int getRightCps() {
        return clean(right);
    }

    private static int clean(ConcurrentLinkedDeque<Long> dq) {
        long n = System.currentTimeMillis();
        while (!dq.isEmpty() && n - dq.peekFirst() > 1000) {
            dq.pollFirst();
        }
        return dq.size();
    }
}
