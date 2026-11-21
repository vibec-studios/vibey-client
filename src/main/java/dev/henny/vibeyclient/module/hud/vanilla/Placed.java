package dev.henny.vibeyclient.module.hud.vanilla;

import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

final class Placed {
    private Placed() {}

    static void draw(Gfx gfx, HudElement e, float dx, float dy, Runnable vanilla) {
        if (!HudLayout.begin(gfx, e)) return;
        try {
            gfx.translate(dx, dy);
            vanilla.run();
        } finally {
            HudLayout.end(gfx);
        }
    }

    static boolean active(HudElement e) {
        return e != null && HudLayout.get(e.id()) == e && e.available() && !HudLayout.hidden(e);
    }
}
