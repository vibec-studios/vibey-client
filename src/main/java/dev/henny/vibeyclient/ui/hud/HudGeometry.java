package dev.henny.vibeyclient.ui.hud;

final class HudGeometry {
    private HudGeometry() {}

    static int boxWidth(HudElement e, HudLayout.Placement p) {
        int n = e.width();
        if (p.bw <= 0 || !e.isWidget()) return n;
        return Math.max(e.freeSize() ? e.minWidth() : n, p.bw);
    }

    static int boxHeight(HudElement e, HudLayout.Placement p) {
        int n = e.height();
        if (p.bh <= 0 || !e.isWidget()) return n;
        return Math.max(e.freeSize() ? e.minHeight() : n, p.bh);
    }

    static HudLayout.Box box(HudElement e, HudLayout.Placement p, int sw, int sh, HudLayout.Box out) {
        float s = p.scale;
        float w = boxWidth(e, p) * s;
        float h = boxHeight(e, p) * s;
        float x = p.anchor.fx() * (sw - w) + p.dx;
        float y = p.anchor.fy() * (sh - h) + p.dy;
        out.x = Math.round(Math.max(0, Math.min(sw - w, x)));
        out.y = Math.round(Math.max(0, Math.min(sh - h, y)));
        out.w = w;
        out.h = h;
        out.scale = s;
        return out;
    }

    static void moveTo(HudElement e, float x, float y, int sw, int sh) {
        HudLayout.Placement p = HudRegistry.placement(e);
        float w = boxWidth(e, p) * p.scale;
        float h = boxHeight(e, p) * p.scale;
        x = Math.max(0, Math.min(sw - w, x));
        y = Math.max(0, Math.min(sh - h, y));
        Anchor a = p.autoAnchor ? Anchor.nearest(x + w / 2f, y + h / 2f, sw, sh) : p.anchor;
        p.anchor = a;
        p.dx = Math.round(x - a.fx() * (sw - w));
        p.dy = Math.round(y - a.fy() * (sh - h));
        p.custom = true;
    }

    static void scaleTo(HudElement e, float scale, int sw, int sh) {
        HudLayout.Placement p = HudRegistry.placement(e);
        HudLayout.Box b = box(e, p, sw, sh, new HudLayout.Box());
        float cx = b.x + b.w / 2f;
        float cy = b.y + b.h / 2f;
        p.scale = clampScale(e, scale);
        moveTo(e, cx - boxWidth(e, p) * p.scale / 2f, cy - boxHeight(e, p) * p.scale / 2f, sw, sh);
    }

    static float clampScale(HudElement e, float s) {
        return Math.max(e.minScale(), Math.min(e.maxScale(), Math.round(s * 40f) / 40f));
    }

    static void setSize(HudElement e, int w, int h) {
        HudLayout.Placement p = HudRegistry.placement(e);
        p.bw = w <= e.width() && !e.freeSize() ? 0 : w;
        p.bh = h <= e.height() && !e.freeSize() ? 0 : h;
        if (p.bw > 0 && p.bw == e.width() && e.freeSize() && p.bh <= 0) p.bw = 0;
        p.custom = true;
    }

    static void setAnchor(HudElement e, Anchor a, int sw, int sh) {
        HudLayout.Placement p = HudRegistry.placement(e);
        HudLayout.Box b = box(e, p, sw, sh, new HudLayout.Box());
        p.autoAnchor = a == null;
        if (a != null) p.anchor = a;
        moveTo(e, b.x, b.y, sw, sh);
    }
}
