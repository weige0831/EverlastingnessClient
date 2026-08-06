package net.everlastingness.client.common.fps;

/**
 * Version-agnostic configuration for the entity-render-distance cull
 * optimisation. The per-version FPS mixin (e.g. {@code MixinEntityCull} on
 * 1.7.10) reads {@link #isEnabled()} and {@link #getCullDistanceSq()} every
 * frame; the {@code FpsOptimizationModule} writes them.
 *
 * <p>Kept dependency-free so it can live in {@code :common}.</p>
 */
public final class EntityCullConfig {

    private static final EntityCullConfig INSTANCE = new EntityCullConfig();

    /** The process-wide config. */
    public static EntityCullConfig get() {
        return INSTANCE;
    }

    /**
     * Default cull distance in blocks. Entities farther than this (relative to
     * the camera) are not rendered when the module is enabled. 48 blocks is a
     * reasonable default that noticeably cuts distant-entity render cost while
     * keeping nearby gameplay fully visible.
     */
    public static final double DEFAULT_CULL_DISTANCE = 48.0;

    private volatile boolean enabled = false;
    private volatile double cullDistance = DEFAULT_CULL_DISTANCE;

    private EntityCullConfig() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Cull distance in blocks (linear). */
    public double getCullDistance() {
        return cullDistance;
    }

    /** Cull distance squared (for cheap per-entity comparison, no sqrt). */
    public double getCullDistanceSq() {
        return cullDistance * cullDistance;
    }

    public void setCullDistance(double cullDistance) {
        this.cullDistance = cullDistance;
    }
}
