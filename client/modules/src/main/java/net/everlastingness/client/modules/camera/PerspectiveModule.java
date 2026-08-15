package net.everlastingness.client.modules.camera;

import net.everlastingness.client.common.module.AbstractModule;

import java.util.logging.Logger;

/**
 * Perspective (camera view) toggle module — allows freely switching between
 * first-person, third-person-back, and third-person-front views with a
 * single keybind, plus the ability to "free-look" (move the camera
 * independently of the player's body direction).
 *
 * <p>On MC 1.7.10, the per-version mixin (MixinEntityRendererPerspective)
 * injects at the render method and, when free-look is active, overrides the
 * yaw/pitch used for camera positioning without affecting player movement
 * yaw. This is a signature Lunar/Badlion feature.</p>
 */
public class PerspectiveModule extends AbstractModule {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/Perspective");

    /** 0 = first person, 1 = third person back, 2 = third person front. */
    private volatile int perspective = 0;
    private volatile boolean freeLook = false;

    /** Free-look camera yaw/pitch offsets. */
    private float freeYaw;
    private float freePitch;

    @Override
    public String getId() { return "perspective"; }

    @Override
    public String getName() { return "Perspective Toggle"; }

    @Override
    public String getDescription() {
        return "Free camera switching + free-look mode.";
    }

    @Override
    public String getCategory() { return "CAMERA"; }

    /** Cycle to next perspective (called by keybind mixin). */
    public void cyclePerspective() {
        perspective = (perspective + 1) % 3;
        LOGGER.info("Perspective: " + (perspective == 0 ? "1st person" :
                      perspective == 1 ? "3rd back" : "3rd front"));
    }

    /** Set the perspective directly (0=1st, 1=3rd-back, 2=3rd-front). */
    public void setPerspective(int value) {
        perspective = ((value % 3) + 3) % 3;
        LOGGER.info("Perspective set: " + (perspective == 0 ? "1st person" :
                      perspective == 1 ? "3rd back" : "3rd front"));
    }

    /** Toggle free-look (called by keybind mixin). */
    public void toggleFreeLook() {
        freeLook = !freeLook;
        LOGGER.info("Free-look: " + (freeLook ? "ON" : "OFF"));
    }

    public int getPerspective() { return perspective; }
    public boolean isFreeLook() { return freeLook; }
    public float getFreeYaw() { return freeYaw; }
    public float getFreePitch() { return freePitch; }
    public void setFreeYaw(float yaw) { this.freeYaw = yaw; }
    public void setFreePitch(float pitch) { this.freePitch = pitch; }

    @Override
    public void onEnable() {
        LOGGER.info("Perspective module enabled");
    }

    @Override
    public void onDisable() {
        freeLook = false;
        perspective = 0;
        LOGGER.info("Perspective module disabled");
    }
}
