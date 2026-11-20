package dev.henny.vibeyclient.ui.hud;

final class Placements {
    private Placements() {}

    static void reset(HudLayout.Placement p, HudElement e) {
        p.anchor = e.defaultAnchor();
        p.autoAnchor = true;
        p.dx = e.defaultX();
        p.dy = e.defaultY();
        p.scale = e.defaultScale();
        p.bw = 0;
        p.bh = 0;
        p.hidden = HudProfiles.active().hiddenByDefault(e);
        p.style.clear();
        p.opts.clear();
        p.custom = false;
        HudCaches.bumpStyle();
    }

    static void copy(HudLayout.Placement p, HudLayout.Placement o) {
        p.anchor = o.anchor;
        p.autoAnchor = o.autoAnchor;
        p.dx = o.dx;
        p.dy = o.dy;
        p.scale = o.scale;
        p.bw = o.bw;
        p.bh = o.bh;
        p.hidden = o.hidden;
        p.style.set(o.style);
        p.opts.clear();
        p.opts.putAll(o.opts);
        p.custom = o.custom;
        HudCaches.bumpStyle();
    }

    static HudLayout.Placement copyOf(HudElement e, HudLayout.Placement o) {
        HudLayout.Placement c = new HudLayout.Placement(e);
        copy(c, o);
        return c;
    }

    static boolean same(HudLayout.Placement a, HudLayout.Placement o) {
        return a.anchor == o.anchor && a.autoAnchor == o.autoAnchor && a.dx == o.dx && a.dy == o.dy && a.scale == o.scale
                && a.bw == o.bw && a.bh == o.bh && a.hidden == o.hidden && a.style.equals(o.style) && a.opts.equals(o.opts);
    }
}
