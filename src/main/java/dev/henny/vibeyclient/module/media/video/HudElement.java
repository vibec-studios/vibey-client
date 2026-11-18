package dev.henny.vibeyclient.module.media.video;

import dev.henny.vibeyclient.ui.Gfx;

/**
 * Something on the HUD that a HUD editor can move and scale. Positions are fractions of the GUI-scaled screen
 * (0..1, the element's top left corner, clamped so it stays on screen), so they survive window resizes. The owner
 * persists the layout itself in {@link #setLayout}. Meant to be replaced by (or adapted to) the Media module's
 * generic {@code ui/hud} element type – see docs/VIDEO.md "Wiring".
 */
public interface HudElement {
    /** Stable id, e.g. {@code "video.overlay"}. */
    String id();

    /** i18n key of the name shown in the editor. */
    String nameKey();

    /** Currently has something to show (the editor shows hidden elements as a placeholder). */
    boolean visible();

    /** Unscaled size in GUI pixels. */
    int width();

    int height();

    float x();

    float y();

    float scale();

    /** Clamp, apply and persist. */
    void setLayout(float x, float y, float scale);

    default float minScale() {
        return 0.25f;
    }

    default float maxScale() {
        return 4f;
    }

    /** Draws at GUI pixel position (x, y), already scaled; {@code editing}: draw a placeholder if not visible. */
    void render(Gfx g, int x, int y, float scale, boolean editing);
}
