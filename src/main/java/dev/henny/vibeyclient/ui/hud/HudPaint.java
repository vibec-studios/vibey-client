package dev.henny.vibeyclient.ui.hud;

import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;

public final class HudPaint {
    public Gfx g;
    public int w;
    public int h;
    public boolean preview;
    public HudStyle.Resolved style;
    public long now;
    HudElement element;
    HudLayout.Placement placement;

    public HudElement element() {
        return element;
    }

    public int align() {
        return switch (style.align) {
            case LEFT -> -1;
            case CENTER -> 0;
            case RIGHT -> 1;
            default -> placement == null ? -1 : placement.anchor.alignX();
        };
    }

    public int alignX(int cw, int pad) {
        int a = align();
        if (a < 0) return pad;
        if (a > 0) return w - pad - cw;
        return (w - cw) / 2;
    }

    public boolean detailed() {
        return style.detailed();
    }

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

    public void background() {
        background(0, 0, w, h);
    }

    public void background(int x, int y, int bw, int bh) {
        HudStyle.Resolved s = style;
        if (s.background) fillShape(x, y, bw, bh, s.bg);
        if (s.border) outlineShape(x, y, bw, bh, Theme.alpha(s.accent, 0.7f));
    }

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

    public void text(String s, int x, int y) {
        switch (style.paint) {
            case ACCENT -> plain(s, x, y, style.accent);
            case RAINBOW -> rainbow(s, x, y, 0xFF);
            default -> plain(s, x, y, style.text);
        }
    }

    public void accentText(String s, int x, int y) {
        if (style.paint == HudStyle.TextPaint.RAINBOW) rainbow(s, x, y, 0xFF);
        else plain(s, x, y, style.accent);
    }

    public void dimText(String s, int x, int y, float alpha) {
        int c = style.paint == HudStyle.TextPaint.ACCENT ? style.accent : style.text;
        plain(s, x, y, Theme.alpha(c, alpha));
    }

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

    public static int rainbowColor(float t, int alpha) {
        float h = t - (float) Math.floor(t);
        return (alpha << 24) | hsv(h, 0.62f, 1f);
    }

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

    private static final String[] CHARS = new String[0x250];

    static {
        for (int i = 0; i < CHARS.length; i++) CHARS[i] = String.valueOf((char) i);
    }
}
