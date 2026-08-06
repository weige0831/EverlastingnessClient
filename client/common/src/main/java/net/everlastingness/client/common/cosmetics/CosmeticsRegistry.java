package net.everlastingness.client.common.cosmetics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Version-agnostic cosmetics registry. Maps a player name to a custom cape
 * texture identifier (resolved to a real texture by the per-version render
 * mixin). In Phase 3c this is a simple per-name override; the launcher will
 * later populate it from a cosmetics backend.
 *
 * <p>Kept dependency-free so it can live in {@code :common} and be shared
 * across MC versions; the texture binding + GL drawing happens in the
 * per-version cape mixin.</p>
 */
public final class CosmeticsRegistry {

    private static final CosmeticsRegistry INSTANCE = new CosmeticsRegistry();

    /** The process-wide cosmetics registry. */
    public static CosmeticsRegistry get() {
        return INSTANCE;
    }

    /**
     * Texture id used to signal "render the bundled default Everlastingness
     * cape" for a player. The per-version mixin maps this to the actual
     * texture resource.
     */
    public static final String DEFAULT_CAPE = "everlastingness:default";

    private final Map<String, String> capesByPlayer = new ConcurrentHashMap<>();

    private CosmeticsRegistry() {
    }

    /** Assign a cape texture id to a player (by username). */
    public void setCape(String username, String capeId) {
        if (username == null || username.isEmpty()) {
            return;
        }
        capesByPlayer.put(username, capeId);
    }

    /** Remove a player's custom cape. */
    public void clearCape(String username) {
        capesByPlayer.remove(username);
    }

    /**
     * The cape texture id for a player, or {@code null} if the player has no
     * custom cape (vanilla behaviour applies).
     */
    public String getCape(String username) {
        return username == null ? null : capesByPlayer.get(username);
    }

    /** Whether cosmetics are globally enabled (tied to the cape module state). */
    private volatile boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
