package dev.henny.vibeyclient.ui.hud;

import dev.henny.vibeyclient.ui.Gfx;

public final class WidgetPainter {
    private WidgetPainter() {}

    private static final HudPaint PAINT = new HudPaint();

    public static boolean update(HudElement e, Gfx g, long now, boolean preview) {
        if (!e.isWidget()) return true;
        try {
            HudPaint p = prepare(e, g, now, preview);
            e.widget().maybeUpdate(p, now);
            return preview || e.widget().shown(p);
        } catch (RuntimeException ex) {
            broken(e, ex);
            return false;
        }
    }

    public static void paint(HudElement e, Gfx g, long now, boolean preview) {
        try {
            if (e.isWidget()) e.widget().paint(prepare(e, g, now, preview));
            else e.render(g, e.width(), e.height(), preview);
        } catch (RuntimeException ex) {
            broken(e, ex);
        }
    }

    private static HudPaint prepare(HudElement e, Gfx g, long now, boolean preview) {
        HudLayout.Placement pl = HudRegistry.placement(e);
        HudPaint p = PAINT;
        p.g = g;
        p.preview = preview;
        p.style = HudCaches.resolved(pl);
        p.now = now;
        p.element = e;
        p.placement = pl;
        p.w = HudGeometry.boxWidth(e, pl);
        p.h = HudGeometry.boxHeight(e, pl);
        return p;
    }

    private static void broken(HudElement e, RuntimeException ex) {
        if (e.brokenLogged) return;
        e.brokenLogged = true;
        dev.henny.vibeyclient.Vibey.LOG.warn("HUD element {} failed", e.id(), ex);
    }
}
