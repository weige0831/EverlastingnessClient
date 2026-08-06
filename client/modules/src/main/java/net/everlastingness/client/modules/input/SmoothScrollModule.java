package net.everlastingness.client.modules.input;

import net.everlastingness.client.common.module.AbstractModule;

import java.util.logging.Logger;

/**
 * Smooth Scrolling module — replaces the vanilla instant-scroll in inventory
 * and creative menus with smooth, accelerated scrolling. A highly visible
 * Lunar/Badlion feature that makes inventory management feel premium.
 *
 * <p>On MC 1.7.10, the per-version mixin (MixinGuiContainerScroll) intercepts
 * mouse wheel events in GuiContainer and applies a smooth offset animation
 * instead of the vanilla instant jump.</p>
 */
public class SmoothScrollModule extends AbstractModule {
    private static final Logger LOGGER = Logger.getLogger("Everlastingness/Scroll");

    /** Scroll animation duration in ms. */
    private int duration = 200;
    /** Current scroll offset (animated). */
    private double currentOffset;
    /** Target scroll offset. */
    private double targetOffset;
    /** Animation start time. */
    private long animStart;

    @Override
    public String getId() { return "smooth_scroll"; }
    @Override
    public String getName() { return "Smooth Scroll"; }
    @Override
    public String getDescription() { return "Smooth scrolling in inventory and menus."; }
    @Override
    public String getCategory() { return "INPUT"; }

    /** Called by mixin when mouse wheel scrolls. Returns adjusted scroll amount. */
    public double onScroll(double vanillaAmount) {
        targetOffset += vanillaAmount;
        animStart = System.currentTimeMillis();
        return vanillaAmount; // mixin applies smooth offset in render
    }

    /** Called each frame by the render mixin to compute the smooth offset. */
    public double getSmoothOffset() {
        long now = System.currentTimeMillis();
        long elapsed = now - animStart;
        if (elapsed >= duration) {
            currentOffset = targetOffset;
        } else {
            double t = (double) elapsed / duration;
            // Ease-out cubic
            double eased = 1 - Math.pow(1 - t, 3);
            currentOffset = currentOffset + (targetOffset - currentOffset) * eased;
        }
        return currentOffset;
    }

    @Override public void onEnable() { LOGGER.info("Smooth scroll enabled"); }
    @Override public void onDisable() {
        currentOffset = 0; targetOffset = 0;
        LOGGER.info("Smooth scroll disabled");
    }
}
