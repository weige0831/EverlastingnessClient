package net.everlastingness.client.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A minimal, thread-safe publish/subscribe event bus. Mixins in the
 * per-version modules post events here; feature modules in {@code :modules}
 * register listeners against concrete event types.
 *
 * <p>This is deliberately dependency-free (no Guava EventBus) so the common
 * module stays a small plain-Java library every version era can share.</p>
 */
public final class EventBus {

    private static final EventBus INSTANCE = new EventBus();

    /** The process-wide bus. */
    public static EventBus get() {
        return INSTANCE;
    }

    private final Map<Class<?>, List<EventListener<?>>> listeners = new ConcurrentHashMap<>();

    private EventBus() {
    }

    /**
     * Register a listener for a specific event type. The listener is invoked
     * synchronously when an event of an assignable type is posted.
     *
     * @param eventType the event class to listen for
     * @param listener  the listener; receives the cast event instance
     * @param <T>       the event type
     */
    public <T extends EverlastingnessEvent> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Remove all listeners registered by the given owner. Ownership is tracked
     * via the listener instance's enclosing module reference.
     */
    public void unsubscribeAll(Object owner) {
        for (Map.Entry<Class<?>, List<EventListener<?>>> entry : listeners.entrySet()) {
            entry.getValue().removeIf(l -> l.isOwnedBy(owner));
        }
    }

    /**
     * Post an event to all registered listeners whose event type is assignable
     * from the event's runtime class. Stops early if a listener cancels a
     * cancellable event.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void post(EverlastingnessEvent event) {
        // Collect matching listener lists. We iterate all keys because events
        // may be listened to by their supertypes.
        for (Map.Entry<Class<?>, List<EventListener<?>>> entry : listeners.entrySet()) {
            if (!entry.getKey().isInstance(event)) {
                continue;
            }
            for (EventListener<?> listener : entry.getValue()) {
                ((EventListener) listener).onEvent(event);
                if (event.isCancelled()) {
                    return;
                }
            }
        }
    }

    /** Snapshot of registered event types (mainly for diagnostics). */
    public List<Class<?>> registeredEventTypes() {
        return Collections.unmodifiableList(new ArrayList<>(listeners.keySet()));
    }

    /** A typed listener callback. */
    @FunctionalInterface
    public interface EventListener<T extends EverlastingnessEvent> {
        void onEvent(T event);

        /** Default ownership check: override per-implementation if needed. */
        default boolean isOwnedBy(Object owner) {
            return false;
        }
    }
}
