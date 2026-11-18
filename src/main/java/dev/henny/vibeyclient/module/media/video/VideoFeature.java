package dev.henny.vibeyclient.module.media.video;

import dev.henny.vibeyclient.ui.Gfx;

import java.util.List;

/**
 * The video screens as a pluggable feature: a host module (today the temporary {@link VideoModule}, later the
 * Media module) forwards its lifecycle here. Mirrors {@code VibeyModule}'s hooks so it can be folded into a
 * {@code MediaFeature} 1:1. All calls on the client thread.
 */
public interface VideoFeature {
    /** Once, after the host's settings were loaded (register net channels, render hooks). */
    void onLoad();

    void onEnable();

    /** Stop every player, release textures, forget remote screens. */
    void onDisable();

    /** 20/s while enabled. */
    void tick();

    /** Every frame while enabled and the HUD is shown (draws the overlay). */
    void renderHud(Gfx g);

    void onWorldJoin();

    void onWorldLeave();

    /** Movable HUD pieces for the HUD editor. */
    List<HudElement> hudElements();
}
