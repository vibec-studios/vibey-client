package dev.henny.vibeyclient.ui.hud;

/**
 * The content of a styled HUD widget (FPS, coordinates, a PvP "Sprinting" indicator, ...). Give the element a
 * factory with {@link HudElement.Builder#widget}; the core then keeps one instance per element (duplicates get their
 * own), draws it on the HUD every frame (no {@code renderHud} code needed), lets the player move, resize and style it
 * in the HUD editor and hides it when the server disabled its rules key.
 *
 * <p>Hot path rules: {@link #paint} runs every frame and must not allocate (no string building, no lists); put the
 * work in {@link #update}, which runs {@link #rate()} times a second (and right away after the style, an option or
 * the preview mode changed). Set {@link #width}/{@link #height} (natural size, unscaled GUI px) in {@code update}.
 * For one or more "label value" lines extend {@link TextWidget} instead.
 */
public abstract class HudWidget {
    /** Natural size in unscaled GUI pixels (set in {@link #update}). */
    protected int width = 40;
    protected int height = 12;

    long nextUpdate;
    boolean dirty = true;
    boolean lastPreview;

    /** Updates per second ({@code 0} = every frame; keep it low: 4 for numbers that change often, 1 for clocks). */
    protected int rate() {
        return 4;
    }

    /**
     * Refreshes cached values (strings, counts, icons) and the natural size. {@code p} carries the style, the options,
     * the preview flag (editor open: show sample values when there is nothing real) and a {@link dev.henny.vibeyclient.ui.Gfx}
     * for measuring text. Allocating here is fine.
     */
    protected abstract void update(HudPaint p);

    /** Draws into (0, 0)–(p.w, p.h) (unscaled box; may be larger than the natural size when resized freely). */
    protected abstract void paint(HudPaint p);

    /** Drawn in game at all right now (e.g. no potion effects = nothing to show). The editor always draws it. */
    protected boolean shown(HudPaint p) {
        return true;
    }

    /** Asks for an {@link #update} before the next paint. */
    public final void invalidate() {
        dirty = true;
    }

    public final int naturalWidth() {
        return Math.max(4, width);
    }

    public final int naturalHeight() {
        return Math.max(4, height);
    }

    /** Called by the layout before painting: updates when due. */
    final void maybeUpdate(HudPaint p, long nowMs) {
        if (p.preview != lastPreview) {
            lastPreview = p.preview;
            dirty = true;
        }
        int r = rate();
        if (!dirty && r > 0 && nowMs < nextUpdate) return;
        dirty = false;
        nextUpdate = nowMs + (r > 0 ? 1000L / r : 0);
        update(p);
    }
}
