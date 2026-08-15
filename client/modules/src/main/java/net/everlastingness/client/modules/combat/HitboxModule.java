package net.everlastingness.client.modules.combat;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Hitbox module — enhanced F3+B hitboxes with per-entity-type visibility,
 * custom line width/color, look-vector line and max render distance,
 * mirroring Lunar's Hitbox mod.
 */
public class HitboxModule extends AbstractModule {
    @Override public String getId() { return "hitbox"; }
    @Override public String getName() { return "Hitbox"; }
    @Override public String getDescription() { return "Enhanced hitboxes for entities."; }
    @Override public String getCategory() { return "COMBAT"; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}
