package dev.henny.vibeyclient.module.nametags;

import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import net.minecraft.network.chat.Component;

import java.util.List;

final class Chips {
    static final int H = 16, GAP = 4, PAD_X = 6;

    record Chip(Object key, Component label, boolean selected) {}

    static int height(Gfx g, List<Chip> chips, int width) {
        if (chips.isEmpty()) return 0;
        int x = 0, lines = 1;
        for (Chip c : chips) {
            int w = width(g, c);
            if (x > 0 && x + w > width) {
                lines++;
                x = 0;
            }
            x += w + GAP;
        }
        return lines * (H + GAP) + GAP;
    }

    static Chip clicked(Ui ui, List<Chip> chips, int left, int y, int right, boolean enabled) {
        Gfx g = ui.g;
        int x = left;
        y += GAP;
        Chip hit = null;
        for (Chip c : chips) {
            int w = width(g, c);
            if (x > left && x + w > right) {
                x = left;
                y += H + GAP;
            }
            if (draw(ui, c, x, y, w) && enabled && !c.selected()) hit = c;
            x += w + GAP;
        }
        return hit;
    }

    private static boolean draw(Ui ui, Chip c, int x, int y, int w) {
        Gfx g = ui.g;
        boolean hover = ui.hover(x, y, w, H);
        int bg = c.selected() ? Theme.alpha(Theme.BRAND, 0.22f) : hover ? Theme.SURFACE3 : Theme.SURFACE;
        g.rect(x, y, w, H, bg);
        g.outline(x, y, w, H, c.selected() ? Theme.BRAND : Theme.LINE);
        g.text(c.label(), x + PAD_X, y + 4, Theme.TEXT);
        return ui.clicked(x, y, w, H);
    }

    private static int width(Gfx g, Chip c) {
        return g.font.width(c.label()) + PAD_X * 2;
    }
}
