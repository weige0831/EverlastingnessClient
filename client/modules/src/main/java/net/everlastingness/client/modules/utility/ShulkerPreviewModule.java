package net.everlastingness.client.modules.utility;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Shulker Preview module — shows shulker box contents in a tooltip when
 * hovering one in the inventory, mirroring Lunar's ShulkerPreview mod.
 */
public class ShulkerPreviewModule extends AbstractModule {
    @Override public String getId() { return "shulker_preview"; }
    @Override public String getName() { return "Shulker Preview"; }
    @Override public String getDescription() { return "Previews shulker contents on hover."; }
    @Override public String getCategory() { return "UTILITY"; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}
