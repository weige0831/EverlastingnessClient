package net.everlastingness.client.modules.visual;

import net.everlastingness.client.common.module.AbstractModule;

import java.util.logging.Logger;

/**
 * Zoom module — optifine-style zoom with smooth FOV transition, keybind
 * activated. One of the most commonly requested features in Lunar/Badlion
 * alternatives.
 *
 * <p>On MC 1.7.7+, the per-version mixin (MixinEntityRendererZoom) intercepts
 * the FOV calculation and, when zoom is active, smoothly transitions the
 * FOV to a configurable zoom level.</p>
 */
public class ZoomModule extends AbstractModule {
    private static final Logger LOGGER = Logger.getLogger("Everlastingness/Zoom");

    /** Zoom FOV multiplier (lower = more zoom). */
    private double zoomMultiplier = 0.25;
    /** Whether zoom key is currently held (set by keybind mixin). */
    private volatile boolean zoomActive = false;
    /** Smooth transition factor (0=normal, 1=full zoom). */
    private double smoothFactor = 0.0;

    @Override
    public String getId() { return "zoom"; }
    @Override
    public String getName() { return "Zoom"; }
    @Override
    public String getDescription() { return "Smooth zoom with configurable FOV."; }
    @Override
    public String getCategory() { return "VISUAL"; }

    public boolean isZoomActive() { return zoomActive; }
    public void setZoomActive(boolean active) { this.zoomActive = active; }

    /** Called each frame by the FOV mixin. Returns adjusted FOV. */
    public float adjustFov(float originalFov, float partialTicks) {
        double target = zoomActive ? 1.0 : 0.0;
        // Smooth transition
        smoothFactor += (target - smoothFactor) * 0.15;
        if (Math.abs(smoothFactor) < 0.001) smoothFactor = 0;
        if (Math.abs(smoothFactor - 1.0) < 0.001) smoothFactor = 1.0;

        double fov = originalFov;
        if (smoothFactor > 0) {
            fov = originalFov * (1.0 - smoothFactor * (1.0 - zoomMultiplier));
        }
        return (float) fov;
    }

    @Override public void onEnable() { LOGGER.info("Zoom enabled"); }
    @Override public void onDisable() { zoomActive = false; smoothFactor = 0; LOGGER.info("Zoom disabled"); }
}
