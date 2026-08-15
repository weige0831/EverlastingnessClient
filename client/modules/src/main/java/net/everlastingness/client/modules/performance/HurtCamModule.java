package net.everlastingness.client.modules.performance;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Hurt Cam module — disables or reduces the hurt camera shake when damaged,
 * mirroring Lunar's HurtCam mod.
 */
public class HurtCamModule extends AbstractModule {
    @Override public String getId() { return "hurt_cam"; }
    @Override public String getName() { return "Hurt Cam"; }
    @Override public String getDescription() { return "Disables hurt camera shake."; }
    @Override public String getCategory() { return "PERFORMANCE"; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}
