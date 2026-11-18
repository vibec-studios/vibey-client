package dev.henny.vibeyclient.ui.hud;

import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;

/**
 * What a {@link HudWidget} gets to draw with: the box size, the resolved style, the widget's options and helpers that
 * draw text and backgrounds in that style. One instance is reused for every widget and frame (never keep it).
 */
public final class HudPaint {
    public Gfx g;
    /** Box size in unscaled GUI pixels (draw into 0..w, 0..h). */
    public int w;
    public int h;
    /** Drawn by the HUD editor: show sample values when there is nothing real to show. */
    public boolean preview;
    /** The resolved style of this widget. */
    public HudStyle.Resolved style;
    /** Milliseconds (System.currentTimeMillis), the same for the whole frame. */
    public long now;
    HudElement element;
    HudLayout.Placement placement;

    public HudElement element() {
        return element;
    }

    /** -1 left, 0 centre, 1 right: the style's alignment, AUTO following the anchor column. */
    public int align() {
        return switch (style.align) {
            case LEFT -> -1;
            case CENTER -> 0;
            case RIGHT -> 1;
            default -> placement == null ? -1 : placement.anchor.alignX();
        };
    }

    /** Left x of content {@code cw} wide inside the box with {@code pad} px padding, following {@link #align()}. */
    public int alignX(int cw, int pad) {
        int a = align();
        if (a < 0) return pad;
        if (a > 0) return w - pad - cw;
        return (w - cw) / 2;
    }

    public boolean detailed() {
        return style.detailed();
    }

    /** The stored value of option {@code key} (its default when unset). */
    public String option(String key) {
        if (placement != null && placement.opts != null) {
            String v = placement.opts.get(key);
            if (v != null) return v;
        }
        if (element != null) {
            for (HudOption o : element.options()) if (o.key.equals(key)) return o.def;
        }
        return "";
    }

    public boolean flag(String key) {
        return "1".equals(option(key));
    }

    // ============================================================================================ drawing

    /** The style's background over the whole box (nothing when switched off). */
    public void background() {
        background(0, 0, w, h);
    }

    public void background(int x, int y, int bw, int bh) {
        HudStyle.Resolved s = style;
        if (s.background) fillShape(x, y, bw, bh, s.bg);
        if (s.border) outlineShape(x, y, bw, bh, Theme.alpha(s.accent, 0.7f));
    }

    /** A filled rectangle in the style's corner shape (square or a 1 px bevel). */
    public void fillShape(int x, int y, int bw, int bh, int argb) {
        if (style.corners == HudStyle.Corners.BEVEL && bw > 2 && bh > 2) {
            g.rect(x + 1, y, bw - 2, 1, argb);
            g.rect(x, y + 1, bw, bh - 2, argb);
            g.rect(x + 1, y + bh - 1, bw - 2, 1, argb);
        } else {
            g.rect(x, y, bw, bh, argb);
        }
    }

    public void outlineShape(int x, int y, int bw, int bh, int argb) {
        if (style.corners == HudStyle.Corners.BEVEL && bw > 2 && bh > 2) {
            g.rect(x + 1, y, bw - 2, 1, argb);
            g.rect(x + 1, y + bh - 1, bw - 2, 1, argb);
            g.rect(x, y + 1, 1, bh - 2, argb);
            g.rect(x + bw - 1, y + 1, 1, bh - 2, argb);
        } else {
            g.outline(x, y, bw, bh, argb);
        }
    }

    /** Main text in the style's text paint (colour, accent or rainbow), with the style's shadow. */
    public void text(String s, int x, int y) {
        switch (style.paint) {
            case ACCENT -> plain(s, x, y, style.accent);
            case RAINBOW -> rainbow(s, x, y, 0xFF);
            default -> plain(s, x, y, style.text);
        }
    }

    /** Text in the accent colour (labels, icons); rainbow when the style's paint is rainbow. */
    public void accentText(String s, int x, int y) {
        if (style.paint == HudStyle.TextPaint.RAINBOW) rainbow(s, x, y, 0xFF);
        else plain(s, x, y, style.accent);
    }

    /** Text in the main colour at {@code alpha} (0..1), for secondary values. */
    public void dimText(String s, int x, int y, float alpha) {
        int c = style.paint == HudStyle.TextPaint.ACCENT ? style.accent : style.text;
        plain(s, x, y, Theme.alpha(c, alpha));
    }

    /** Text in a fixed ARGB colour, with the style's shadow. */
    public void plain(String s, int x, int y, int argb) {
        if (s == null || s.isEmpty()) return;
        if (style.shadow) g.textShadow(s, x, y, argb);
        else g.text(s, x, y, argb);
    }

    private void rainbow(String s, int x, int y, int alpha) {
        if (s == null || s.isEmpty()) return;
        float base = (now % 4000L) / 4000f;
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= CHARS.length) {
                // outside the cached range: the rest in one colour (no allocation for the common case)
                plain(s.substring(i), cx, y, rainbowColor(base + cx * 0.008f, alpha));
                return;
            }
            String one = CHARS[c];
            int col = rainbowColor(base + cx * 0.008f, alpha);
            if (style.shadow) g.textShadow(one, cx, y, col);
            else g.text(one, cx, y, col);
            cx += g.width(one);
        }
    }

    /** A rainbow colour at {@code t} (wraps at 1) in the Vibe tones (soft, not neon). */
    public static int rainbowColor(float t, int alpha) {
        float h = t - (float) Math.floor(t);
        return (alpha << 24) | hsv(h, 0.62f, 1f);
    }

    /** RGB of hue/saturation/value in 0..1. */
    public static int hsv(float h, float s, float v) {
        h = (h - (float) Math.floor(h)) * 6f;
        int i = (int) h;
        float f = h - i;
        float p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        float r, gg, b;
        switch (i) {
            case 0 -> { r = v; gg = t; b = p; }
            case 1 -> { r = q; gg = v; b = p; }
            case 2 -> { r = p; gg = v; b = t; }
            case 3 -> { r = p; gg = q; b = v; }
            case 4 -> { r = t; gg = p; b = v; }
            default -> { r = v; gg = p; b = q; }
        }
        return (Math.round(r * 255) << 16) | (Math.round(gg * 255) << 8) | Math.round(b * 255);
    }

    /** One-character strings for the rainbow paint (no allocation per frame). */
    private static final String[] CHARS = new String[0x250];

    static {
        for (int i = 0; i < CHARS.length; i++) CHARS[i] = String.valueOf((char) i);
    }
}
