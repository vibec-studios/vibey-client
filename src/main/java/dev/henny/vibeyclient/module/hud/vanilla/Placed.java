package dev.henny.vibeyclient.module.hud.vanilla;

import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

final class Placed {
    private Placed() {}

    static void draw(GuiGraphics g, HudElement e, float dx, float dy, Runnable vanilla) {
        Gfx gfx = new Gfx(g, Minecraft.getInstance().font);
        if (!HudLayout.begin(gfx, e)) return;
        try {
            g.pose().translate(dx, dy);
            vanilla.run();
        } finally {
            HudLayout.end(gfx);
        }
    }

    static boolean active(HudElement e) {
        return e != null && HudLayout.get(e.id()) == e && e.available() && !HudLayout.hidden(e);
    }
}
