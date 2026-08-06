package net.everlastingness.client.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EventBus} — the version-agnostic pub/sub backbone.
 *
 * <p>{@link EventBus} is a process-wide singleton ({@link EventBus#get()}); each
 * test creates listener instances owned by a per-test sentinel object and calls
 * {@link EventBus#unsubscribeAll(Object)} in {@link AfterEach} so listeners
 * don't bleed across tests.</p>
 *
 * <p>These run on plain Java (no Mixin / MC), proving the common framework
 * logic is correct independent of the runtime injection layer.</p>
 */
class EventBusTest {

    /** Per-test owner sentinel, used to clean up listeners via unsubscribeAll. */
    private final Object owner = new Object();

    @AfterEach
    void cleanup() {
        EventBus.get().unsubscribeAll(owner);
    }

    /** A concrete event used by the tests. */
    static class TickEvent extends EverlastingnessEvent {
        final int value;
        TickEvent(int value) { this.value = value; }
    }

    /** A cancellable event variant. */
    static class CancellableEvent extends EverlastingnessEvent {
        @Override public boolean isCancellable() { return true; }
    }

    /** An owner-aware listener that records the last received value. */
    private static final class RecordingListener implements EventBus.EventListener<TickEvent> {
        final AtomicInteger owner;
        final Object ownerMarker;
        final AtomicInteger last = new AtomicInteger(Integer.MIN_VALUE);
        final AtomicInteger count = new AtomicInteger();
        RecordingListener(Object ownerMarker) { this.ownerMarker = ownerMarker; this.owner = null; }
        @Override public void onEvent(TickEvent event) { last.set(event.value); count.incrementAndGet(); }
        @Override public boolean isOwnedBy(Object o) { return o == ownerMarker; }
    }

    @Test
    void subscriberReceivesPostedEvent() {
        RecordingListener l = new RecordingListener(owner);
        EventBus.get().subscribe(TickEvent.class, l);

        EventBus.get().post(new TickEvent(42));

        assertEquals(42, l.last.get(), "subscriber should receive the posted value");
    }

    @Test
    void multipleSubscribersAllReceive() {
        RecordingListener a = new RecordingListener(owner);
        RecordingListener b = new RecordingListener(owner);
        EventBus.get().subscribe(TickEvent.class, a);
        EventBus.get().subscribe(TickEvent.class, b);

        EventBus.get().post(new TickEvent(1));
        EventBus.get().post(new TickEvent(2));

        assertEquals(2, a.count.get());
        assertEquals(2, b.count.get());
    }

    @Test
    void cancellingStopsLaterSubscribers() {
        AtomicInteger afterCancel = new AtomicInteger();
        // First listener cancels; second must not run.
        EventBus.EventListener<CancellableEvent> canceller = new EventBus.EventListener<CancellableEvent>() {
            @Override public void onEvent(CancellableEvent e) { e.cancel(); }
            @Override public boolean isOwnedBy(Object o) { return o == owner; }
        };
        EventBus.EventListener<CancellableEvent> after = new EventBus.EventListener<CancellableEvent>() {
            @Override public void onEvent(CancellableEvent e) { afterCancel.incrementAndGet(); }
            @Override public boolean isOwnedBy(Object o) { return o == owner; }
        };
        EventBus.get().subscribe(CancellableEvent.class, canceller);
        EventBus.get().subscribe(CancellableEvent.class, after);

        EventBus.get().post(new CancellableEvent());

        assertEquals(0, afterCancel.get(),
                "a cancelled event must not reach subscribers registered after the cancelling one");
    }

    @Test
    void cancellingNonCancellableThrows() {
        EventBus.EventListener<TickEvent> canceller = new EventBus.EventListener<TickEvent>() {
            @Override public void onEvent(TickEvent e) { e.cancel(); }
            @Override public boolean isOwnedBy(Object o) { return o == owner; }
        };
        EventBus.get().subscribe(TickEvent.class, canceller);

        boolean threw = false;
        try {
            EventBus.get().post(new TickEvent(0));
        } catch (IllegalStateException expected) {
            threw = true;
        }
        assertTrue(threw, "cancelling a non-cancellable event must throw IllegalStateException");
    }

    @Test
    void unsubscribeStopsDelivery() {
        RecordingListener l = new RecordingListener(owner);
        EventBus.get().subscribe(TickEvent.class, l);

        EventBus.get().post(new TickEvent(0));
        EventBus.get().unsubscribeAll(owner);
        EventBus.get().post(new TickEvent(0));

        assertEquals(1, l.count.get(), "after unsubscribeAll the listener must not be called");
    }

    @Test
    void eventIsNotCancelledByDefault() {
        EverlastingnessEvent e = new TickEvent(0);
        assertFalse(e.isCancelled());
        assertFalse(e.isCancellable());
    }
}
