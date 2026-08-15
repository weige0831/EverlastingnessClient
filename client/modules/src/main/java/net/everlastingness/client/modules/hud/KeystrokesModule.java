package net.everlastingness.client.modules.hud;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Keystrokes module — on-screen WASD/LMB/RMB/spacebar display with CPS
 * integration, mirroring Lunar's Keystrokes mod.
 */
public class KeystrokesModule extends AbstractModule {
    @Override public String getId() { return "keystrokes"; }
    @Override public String getName() { return "Keystrokes"; }
    @Override public String getDescription() { return "Shows your key and mouse presses on screen."; }
    @Override public String getCategory() { return "HUD"; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}
