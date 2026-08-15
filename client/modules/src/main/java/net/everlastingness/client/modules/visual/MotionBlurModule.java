package net.everlastingness.client.modules.visual;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Motion Blur module — adds a subtle motion blur effect when moving or
 * rotating the camera, mirroring Lunar's MotionBlur mod.
 */
public class MotionBlurModule extends AbstractModule {
    /** 0.0 (off) .. 1.0 (max) blend strength. */
    private double amount = 0.5;

    @Override public String getId() { return "motion_blur"; }
    @Override public String getName() { return "Motion Blur"; }
    @Override public String getDescription() { return "Adds motion blur when moving."; }
    @Override public String getCategory() { return "VISUAL"; }

    public double getAmount() { return amount; }
    public void setAmount(double a) { this.amount = Math.max(0, Math.min(1, a)); }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}
