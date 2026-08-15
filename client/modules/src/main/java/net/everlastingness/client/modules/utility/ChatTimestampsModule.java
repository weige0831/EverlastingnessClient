package net.everlastingness.client.modules.utility;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Chat Timestamps module — prefixes chat messages with the real-world time
 * they were received, mirroring Lunar's Chat timestamps option.
 */
public class ChatTimestampsModule extends AbstractModule {
    private boolean use24h = true;

    @Override public String getId() { return "chat_timestamps"; }
    @Override public String getName() { return "Chat Timestamps"; }
    @Override public String getDescription() { return "Adds timestamps to chat messages."; }
    @Override public String getCategory() { return "UTILITY"; }

    public boolean is24h() { return use24h; }
    public void set24h(boolean b) { this.use24h = b; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}
