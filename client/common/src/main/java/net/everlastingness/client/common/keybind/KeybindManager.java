package net.everlastingness.client.common.keybind;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Version-agnostic keybind registry. Feature code registers a callback against
 * an LWJGL key code (e.g. {@code org.lwjgl.input.Keyboard.KEY_R}); a
 * per-version mixin (e.g. {@code MixinMinecraftRunTick} on 1.7.10) calls
 * {@link #poll()} once per game tick.
 *
 * <p>Each registration tracks its own edge state so the callback fires once on
 * the rising edge (key just pressed), not every tick the key is held. This
 * mirrors the "on press" behaviour of most client feature keybinds (toggle a
 * module, open a GUI, etc.).</p>
 *
 * <p>Kept dependency-free (no LWJGL import) so it can live in {@code :common}
 * and be shared across MC versions; the int key codes are passed in by the
 * caller from whichever input library the version uses.</p>
 */
public final class KeybindManager {

    private static final KeybindManager INSTANCE = new KeybindManager();

    /** The process-wide manager. */
    public static KeybindManager get() {
        return INSTANCE;
    }

    /**
     * A registered keybind. The {@code wasDown} flag tracks the previous poll
     * state so the callback fires on the rising edge only.
     */
    private static final class Binding {
        final int keyCode;
        final Consumer<KeyAction> callback;
        final String description;
        boolean wasDown = false;

        Binding(int keyCode, Consumer<KeyAction> callback, String description) {
            this.keyCode = keyCode;
            this.callback = callback;
            this.description = description;
        }
    }

    private final Map<String, Binding> bindings = new ConcurrentHashMap<>();
    private KeyStateProvider stateProvider = keyCode -> false;

    private KeybindManager() {
    }

    /**
     * Set the source of "is this key currently down" truth. Wired by the
     * per-version mixin (e.g. LWJGL {@code Keyboard.isKeyDown} on 1.7.10).
     */
    public void setStateProvider(KeyStateProvider provider) {
        this.stateProvider = provider;
    }

    /**
     * Register a keybind. When the key transitions from up to down (rising
     * edge), {@code callback} is invoked once with {@link KeyAction#PRESSED}.
     * Re-registering the same id replaces the binding.
     *
     * @param id          stable unique id, e.g. {@code "toggle_hud"}
     * @param keyCode     the input-library key code (e.g. LWJGL KEY_R)
     * @param callback    action to run on press
     * @param description human-readable label
     */
    public void register(String id, int keyCode, Consumer<KeyAction> callback, String description) {
        bindings.put(id, new Binding(keyCode, callback, description));
    }

    /** Remove a keybind by id. */
    public void unregister(String id) {
        bindings.remove(id);
    }

    /**
     * Poll every registered keybind and fire callbacks on rising edges.
     * Called once per game tick by the per-version mixin.
     */
    public void poll() {
        for (Binding b : bindings.values()) {
            boolean down = stateProvider.isKeyDown(b.keyCode);
            if (down && !b.wasDown) {
                try {
                    b.callback.accept(KeyAction.PRESSED);
                } catch (Throwable t) {
                    // A faulty callback must never break the tick loop.
                    System.err.println("[Everlastingness] Keybind '" + b.description + "' callback failed: " + t);
                }
            }
            b.wasDown = down;
        }
    }

    /** Immutable snapshot of registered keybind ids. */
    public Iterable<String> ids() {
        return new LinkedHashMap<>(bindings).keySet();
    }

    /** Read a binding's description by id (for UI display). */
    public String description(String id) {
        Binding b = bindings.get(id);
        return b == null ? null : b.description;
    }

    /** Action delivered to a keybind callback. */
    public enum KeyAction {
        /** The key transitioned from up to down this tick. */
        PRESSED
    }

    /** Source of per-key "down" state, supplied by the per-version input mixin. */
    @FunctionalInterface
    public interface KeyStateProvider {
        boolean isKeyDown(int keyCode);
    }
}
