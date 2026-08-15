package net.everlastingness.client.modules.visual;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Nick Hider module — hides your own (and optionally others') names and skin
 * in nametags and the tab list, mirroring Lunar's NickHider mod.
 */
public class NickHiderModule extends AbstractModule {
    private boolean hideOthers = false;

    @Override public String getId() { return "nick_hider"; }
    @Override public String getName() { return "Nick Hider"; }
    @Override public String getDescription() { return "Hides player names."; }
    @Override public String getCategory() { return "VISUAL"; }

    public boolean isHidingOthers() { return hideOthers; }
    public void setHidingOthers(boolean b) { this.hideOthers = b; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}
