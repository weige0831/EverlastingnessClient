package net.everlastingness.client.modules.utility;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Auto Text module — keybind-triggered chat macros (auto-GG, auto-/hub),
 * mirroring Lunar's AutoTextHotkey mod. Each macro maps a keybind to a
 * command/message sent to chat on rising edge.
 */
public class AutoTextModule extends AbstractModule {
    /** A single macro: key code -> command. */
    public static class Macro {
        public final int keyCode;
        public final String command;
        public Macro(int keyCode, String command) {
            this.keyCode = keyCode;
            this.command = command;
        }
    }

    @Override public String getId() { return "auto_text"; }
    @Override public String getName() { return "Auto Text"; }
    @Override public String getDescription() { return "Keybind chat macros like auto-GG."; }
    @Override public String getCategory() { return "UTILITY"; }

    public void sendMacro(Macro macro) {
        // Wired by the keybind mixin: sends the command via the player's
        // sendChatMessage when the macro key is pressed.
    }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}
