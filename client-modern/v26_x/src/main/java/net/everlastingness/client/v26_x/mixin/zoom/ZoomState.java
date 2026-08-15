package net.everlastingness.client.v26_x.mixin.zoom;

/**
 * Zoom state holder. Mixin classes cannot declare non-private static members
 * (InvalidMixinException at apply time), so the flag and smoothing live here.
 */
public final class ZoomState {

    private static volatile boolean zoomActive = false;
    private static double smooth = 0.0;
    private static final double ZOOM_FACTOR = 0.3;

    private ZoomState() {
    }

    public static void setZoomActive(boolean a) {
        zoomActive = a;
    }

    public static boolean isZoomActive() {
        return zoomActive;
    }

    public static double targetFov(double base) {
        return zoomActive ? base * ZOOM_FACTOR : base;
    }

    public static double smoothTowards(double target, double current, float delta) {
        double t = 1.0 - Math.pow(0.001, delta);
        smooth = current + (target - current) * t;
        return smooth;
    }
}
