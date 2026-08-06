package net.everlastingness.client.common.event;

import net.everlastingness.client.common.EverlastingnessEvent;

/**
 * Fired each render frame. Per-version mixins (in {@code v1_7_10} etc.) post
 * this from Minecraft's renderer. Feature modules such as the HUD listen for
 * it to draw overlays.
 *
 * <p>Phase 0 note: the actual posting call is wired by the per-version
 * mixin (e.g. {@code MixinEntityRenderer}); the common module only declares
 * the event contract.</p>
 */
public class RenderTickEvent extends EverlastingnessEvent {

    /** Partial-tick interpolation value Minecraft passes to renderers (0..1). */
    private final float partialTicks;

    public RenderTickEvent(float partialTicks) {
        this.partialTicks = partialTicks;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}
