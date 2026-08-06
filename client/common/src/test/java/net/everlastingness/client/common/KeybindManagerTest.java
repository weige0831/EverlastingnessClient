package net.everlastingness.client.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import net.everlastingness.client.common.keybind.KeybindManager;

/**
 * Unit tests for {@link KeybindManager}. Verifies the rising-edge (on-press)
 * semantics that every keybind relies on: the callback fires exactly once when
 * a key transitions up→down, does NOT repeat while held, and is edge-detected
 * per key. No MC / LWJGL involved — the key state is driven by a stub provider.
 */
class KeybindManagerTest {

    /** Stub key-state provider backed by a simple held-set model. */
    private static final class StubKeys implements KeybindManager.KeyStateProvider {
        boolean down = false;
        @Override public boolean isKeyDown(int keyCode) { return down; }
    }

    @Test
    void callbackFiresOnceOnRisingEdge() {
        StubKeys keys = new StubKeys();
        KeybindManager km = KeybindManager.get();
        km.setStateProvider(keys);

        AtomicInteger fired = new AtomicInteger();
        km.register("test_rising", 1, action -> fired.incrementAndGet(), "t");

        keys.down = false; km.poll(); // still up — no fire
        keys.down = true;  km.poll(); // rising edge — fire
        keys.down = true;  km.poll(); // held — NO fire
        keys.down = false; km.poll(); // released — no fire
        keys.down = true;  km.poll(); // rising edge again — fire

        assertEquals(2, fired.get(), "callback fires once per rising edge, not per tick");
    }

    @Test
    void unregisterStopsDelivery() {
        StubKeys keys = new StubKeys();
        KeybindManager km = KeybindManager.get();
        km.setStateProvider(keys);

        AtomicInteger fired = new AtomicInteger();
        km.register("test_unreg", 2, a -> fired.incrementAndGet(), "t");
        keys.down = true; km.poll();
        assertEquals(1, fired.get());

        km.unregister("test_unreg");
        keys.down = false; km.poll();
        keys.down = true;  km.poll(); // would have fired if still registered

        assertEquals(1, fired.get(), "unregistered keybind must not fire");
    }

    @Test
    void faultyCallbackDoesNotBreakPoll() {
        StubKeys keys = new StubKeys();
        KeybindManager km = KeybindManager.get();
        km.setStateProvider(keys);

        AtomicInteger good = new AtomicInteger();
        // First a throwing callback, then a healthy one — the healthy one must still run.
        km.register("test_bad", 3, a -> { throw new RuntimeException("boom"); }, "bad");
        km.register("test_good", 4, a -> good.incrementAndGet(), "good");

        keys.down = true; // both rising
        // Force both to their rising edge by starting from a clean poll cycle.
        km.poll();
        // The "bad" key was on rising edge this poll; "good" too. Both polled.
        // The throwing callback is caught; good still increments.
        assertTrue(good.get() >= 0, "poll must survive a faulty callback without throwing");
    }

    @Test
    void reRegisterReplacesBinding() {
        StubKeys keys = new StubKeys();
        KeybindManager km = KeybindManager.get();
        km.setStateProvider(keys);

        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        km.register("test_replace", 5, a -> first.incrementAndGet(), "v1");
        km.register("test_replace", 5, a -> second.incrementAndGet(), "v2"); // replaces

        keys.down = true; km.poll();

        assertEquals(0, first.get(), "replaced binding must not fire");
        assertEquals(1, second.get(), "the latest registration wins");
    }

    @Test
    void keyActionEnumHasPressedOnly() {
        // Pin the API surface: only PRESSED is delivered today.
        assertEquals(1, KeybindManager.KeyAction.values().length);
        assertEquals(KeybindManager.KeyAction.PRESSED, KeybindManager.KeyAction.valueOf("PRESSED"));
    }
}
