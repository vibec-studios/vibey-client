package dev.henny.vibeyclient.module.skins.ui;

import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.resources.Identifier;

final class SkinFigure {
    static final int W = 16, H = 32;

    private SkinFigure() {}

    static void draw(Gfx g, Identifier tex, int x, int y, int scale, boolean slim) {
        int arm = slim ? 3 : 4;
        part(g, tex, x, y, scale, 4, 20, 4, 12, 4, 20, 4, 36);
        part(g, tex, x, y, scale, 8, 20, 4, 12, 20, 52, 4, 52);
        part(g, tex, x, y, scale, 4, 8, 8, 12, 20, 20, 20, 36);
        part(g, tex, x, y, scale, 4 - arm, 8, arm, 12, 44, 20, 44, 36);
        part(g, tex, x, y, scale, 12, 8, arm, 12, 36, 52, 52, 52);
        part(g, tex, x, y, scale, 4, 0, 8, 8, 8, 8, 40, 8);
    }

    private static void part(Gfx g, Identifier tex, int x, int y, int s, int dx, int dy, int w, int h, int u, int v, int ou, int ov) {
        g.image(tex, x + dx * s, y + dy * s, w * s, h * s, u, v, w, h, 64, 64);
        g.image(tex, x + dx * s, y + dy * s, w * s, h * s, ou, ov, w, h, 64, 64);
    }
}
