package net.everlastingness.client.common;

/**
 * Marker base class for all Everlastingness events posted on the
 * {@link EventBus}. Concrete event types live alongside the modules that fire
 * them (e.g. {@code RenderTickEvent} in the HUD module).
 */
public abstract class EverlastingnessEvent {
    private boolean cancelled;

    /** Whether downstream handlers should be skipped (cancellable events only). */
    public boolean isCancelled() {
        return cancelled;
    }

    /** Cancel this event. Only meaningful for events that override {@link #isCancellable()}. */
    public void cancel() {
        if (!isCancellable()) {
            throw new IllegalStateException("Event " + getClass().getSimpleName() + " is not cancellable");
        }
        this.cancelled = true;
    }

    /** Override to return {@code true} for events that support cancellation. */
    public boolean isCancellable() {
        return false;
    }
}
