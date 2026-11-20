package dev.henny.vibeyclient.ui.hud;

import dev.henny.vibeyclient.ui.Gfx;

final class PlainDraw {
    private PlainDraw() {}

    private static final HudLayout.Box TMP = new HudLayout.Box();
    private static int depth;

    static boolean begin(Gfx g, HudElement e) {
        HudLayout.Placement p = HudRegistry.placement(e);
        if (p.hidden || HudCaches.serverDisabled(e, p)) return false;
        HudLayout.Box b = HudGeometry.box(e, p, g.screenWidth(), g.screenHeight(), TMP);
        g.push();
        g.translate(b.x, b.y);
        g.scale(b.scale, b.scale);
        depth++;
        return true;
    }

    static void end(Gfx g) {
        if (depth <= 0) return;
        depth--;
        g.pop();
    }
}
