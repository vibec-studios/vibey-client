package dev.henny.vibeyclient.ui.hud.editor;

import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.hud.edit.Guides;
import dev.henny.vibeyclient.ui.hud.edit.MarqueeGesture;
import dev.henny.vibeyclient.ui.hud.edit.PointerTool;
import dev.henny.vui.hud.SnapGuide;

final class GuideLines {
    private static final int PINK = 0xFFFF5FA8;

    void draw(Pen p, Guides g, PointerTool tool, int sw, int sh) {
        if (tool.busy()) {
            for (SnapGuide l : g.lines()) line(p, l, sw, sh);
            for (Guides.Readout r : g.readouts()) readout(p, r);
            if (g.hasAnchorDot()) dot(p, g.anchorX(), g.anchorY());
        }
        MarqueeGesture m = tool.marquee();
        if (m != null) {
            float w = m.right() - m.left(), h = m.bottom() - m.top();
            p.rect(m.left(), m.top(), w, h, Theme.alpha(Theme.BRAND, 0.12f));
            p.outline(m.left(), m.top(), Math.max(1, w), Math.max(1, h), Theme.alpha(Theme.BRAND, 0.9f));
        }
    }

    void sizeChip(Pen p, Guides g, float cx, float y, int sw) {
        String s = g.sizeChip();
        if (s == null) return;
        float w = p.width(s) + 8;
        p.chip(s, Math.round(Math.max(2, Math.min(sw - w - 2, cx - w / 2))), Math.round(y), Theme.BRAND, 0xFFFFFFFF);
    }

    private static void line(Pen p, SnapGuide l, int sw, int sh) {
        int at = Math.round(l.position());
        int from = Math.round(l.from()), to = Math.round(l.to());
        if (l.vertical()) p.rect(Math.min(sw - 1, at), from, 1, Math.max(1, to - from), Theme.BRAND2);
        else p.rect(from, Math.min(sh - 1, at), Math.max(1, to - from), 1, Theme.BRAND2);
    }

    private static void readout(Pen p, Guides.Readout r) {
        int c = 0xCCFF5FA8;
        String s = Integer.toString(Math.abs(r.value()));
        float tw = p.width(s) + 6;
        if (r.horizontal()) {
            int a = Math.round(Math.min(r.x1(), r.x2())), b = Math.round(Math.max(r.x1(), r.x2())), y = Math.round(r.y1());
            if (b - a < 2) return;
            p.rect(a, y, b - a, 1, c);
            p.rect(a, y - 2, 1, 5, c);
            p.rect(b - 1, y - 2, 1, 5, c);
            float tx = (a + b) / 2f - tw / 2;
            p.rect(Math.round(tx), y - 12, tw, 10, PINK);
            p.text(s, Math.round(tx) + 3, y - 11, 0xFFFFFFFF);
        } else {
            int a = Math.round(Math.min(r.y1(), r.y2())), b = Math.round(Math.max(r.y1(), r.y2())), x = Math.round(r.x1());
            if (b - a < 2) return;
            p.rect(x, a, 1, b - a, c);
            p.rect(x - 2, a, 5, 1, c);
            p.rect(x - 2, b - 1, 5, 1, c);
            int ty = (a + b) / 2 - 5;
            p.rect(x + 3, ty, tw, 10, PINK);
            p.text(s, x + 6, ty + 1, 0xFFFFFFFF);
        }
    }

    private static void dot(Pen p, float x, float y) {
        int ax = Math.round(x), ay = Math.round(y);
        p.rect(ax - 2, ay - 2, 5, 5, Theme.BRAND2);
        p.rect(ax - 1, ay - 1, 3, 3, 0xFF0B0B0E);
    }
}
