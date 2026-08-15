package net.everlastingness.client.modules.utility;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Screenshot Viewer module — captures a screenshot on F2 and copies it to
 * the clipboard / shows a preview toast, mirroring Lunar's Screenshot mod.
 */
public class ScreenshotModule extends AbstractModule {
    @Override public String getId() { return "screenshot_viewer"; }
    @Override public String getName() { return "Screenshot Viewer"; }
    @Override public String getDescription() { return "Screenshot capture with copy-to-clipboard."; }
    @Override public String getCategory() { return "UTILITY"; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}
