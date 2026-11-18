package dev.henny.vibeyclient.api.service;

import dev.henny.vibeyclient.ui.Gfx;

/**
 * The 3D player preview on the Vibey title screen (right side) and wherever else a menu wants to show the local
 * player with their cosmetics. Provided by the avatar/cosmetics module:
 * {@code Services.register(MenuPreview.class, impl)}; the title screen looks it up every frame and draws a
 * placeholder (the seal) while none is registered. All calls happen on the render thread, coordinates are GUI pixels.
 */
public interface MenuPreview {
    /**
     * Draws the preview into the box {@code x, y, w, h} (the player should stand on the box's bottom edge, centred,
     * as large as fits). {@code mouseX/Y} for "looks at the cursor".
     */
    void render(Gfx g, int x, int y, int w, int h, int mouseX, int mouseY, float delta);

    /** Mouse pressed inside the preview box; true if used (e.g. starts a drag-rotate). */
    default boolean mouseClicked(double x, double y, int button) {
        return false;
    }

    /** Drag after a used click ({@code dx/dy} in GUI pixels since the last call); true if used. */
    default boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return false;
    }

    default boolean mouseReleased(double x, double y, int button) {
        return false;
    }

    /** Wheel over the preview box (positive = up); true if used (e.g. zoom). */
    default boolean mouseScrolled(double x, double y, double amount) {
        return false;
    }

    /** The screen showing the preview opened ({@code place}: "title", "wardrobe", …). */
    default void onShow(String place) {}

    /** The screen showing the preview closed. */
    default void onHide(String place) {}
}
