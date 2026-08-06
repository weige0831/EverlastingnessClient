package net.everlastingness.client.v1_7_10.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/**
 * In-game configuration GUI for the Everlastingness client on MC 1.7.10.
 *
 * <p>A {@link GuiScreen} that lists every registered feature {@link Module} with
 * a toggle button showing its on/off state, plus a "Done" button to close. This
 * is the user-facing control surface — equivalent to Lunar/Badlion's mod
 * settings menu — and demonstrates real interaction with MC's GUI stack:</p>
 * <ul>
 *   <li>{@link #initGui()} builds the buttons and lays them out,</li>
 *   <li>{@link #drawScreen} paints a dark panel + a title + the button labels,</li>
 *   <li>{@link #actionPerformed} toggles the matching module's enabled state.</li>
 * </ul>
 *
 * <p>Opened by a keybind (registered in {@code ClientTweaker}, e.g. RIGHT_SHIFT)
 * via {@link Minecraft#displayGuiScreen(GuiScreen)}. No mixin is needed to
 * display it — {@code displayGuiScreen} is a public Minecraft method.</p>
 *
 * <p>MC API verified against the decompiled 1.7.10 source: GuiScreen exposes
 * {@code public int width/height}, {@code protected List buttonList},
 * {@code protected Minecraft mc}; GuiButton(id, x, y, text) constructor;
 * {@code actionPerformed(GuiButton)} for clicks.</p>
 */
public class ModuleConfigGuiScreen extends GuiScreen {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 24;
    private static final int DONE_BUTTON_ID = 9999;

    /** Snapshot of modules taken at {@link #initGui()} time. */
    private List<Module> modules = new ArrayList<>();

    @Override
    public void initGui() {
        EverlastingnessClient client = EverlastingnessClient.get();
        modules = new ArrayList<>();
        if (client != null) {
            for (Module m : client.modules()) {
                modules.add(m);
            }
        }

        buttonList.clear();
        int centerX = this.width / 2 - BUTTON_WIDTH / 2;
        int y = 50;

        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            // Button id = module index (0..n-1). Label shows name + state.
            GuiButton btn = new GuiButton(i, centerX, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                    labelFor(m));
            btn.enabled = true;
            buttonList.add(btn);
            y += SPACING;
        }

        // "Done" button at the bottom.
        int doneY = Math.max(y + 10, this.height - 30);
        buttonList.add(new GuiButton(DONE_BUTTON_ID, centerX, doneY,
                BUTTON_WIDTH, BUTTON_HEIGHT, "Done"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Dark translucent background, then the default button rendering.
        drawDefaultBackground();

        FontRenderer font = this.fontRendererObj != null ? this.fontRendererObj
                : (this.mc != null ? this.mc.fontRenderer : null);
        if (font != null) {
            String title = "Everlastingness — Modules";
            font.drawStringWithShadow(title,
                    this.width / 2 - font.getStringWidth(title) / 2, 20, 0xFFFFFFFF);

            String hint = "Click a module to toggle · Esc to close";
            font.drawStringWithShadow(hint,
                    this.width / 2 - font.getStringWidth(hint) / 2, 34, 0xFF9AA3B2);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == DONE_BUTTON_ID) {
            this.mc.displayGuiScreen(null);
            return;
        }
        // Map the button back to its module and flip its enabled state.
        if (button.id >= 0 && button.id < modules.size()) {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client != null) {
                Module m = modules.get(button.id);
                if (m.isEnabled()) {
                    client.disableModule(m.getId());
                } else {
                    client.enableModule(m.getId());
                }
                button.displayString = labelFor(m);
            }
        }
    }

    @Override
    public void onGuiClosed() {
        // Persist is handled by EverlastingnessClient's config on toggle; nothing extra.
    }

    /** Build a button label "{name}: [ON|OFF]" for a module. */
    private static String labelFor(Module m) {
        return m.getName() + ": " + (m.isEnabled() ? "[ON]" : "[OFF]");
    }
}
