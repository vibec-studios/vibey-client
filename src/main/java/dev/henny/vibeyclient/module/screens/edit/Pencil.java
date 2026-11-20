package dev.henny.vibeyclient.module.screens.edit;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.screens.ScreensModule;
import dev.henny.vibeyclient.module.screens.apply.ScreenSession;
import dev.henny.vibeyclient.module.screens.apply.WidgetScan;
import dev.henny.vibeyclient.module.screens.core.Box;
import dev.henny.vibeyclient.module.screens.core.Spots;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public final class Pencil {
    static final int SIZE = 12;

    private Pencil() {}

    public static Box box(ScreenSession s) {
        List<Box> taken = new ArrayList<>();
        for (Box b : WidgetScan.taken(s)) taken.add(b.shifted(s.offsetX(), s.offsetY()));
        return Spots.pick(s.width(), s.height(), SIZE, taken);
    }

    public static boolean hit(ScreenSession s, double x, double y) {
        return box(s).contains(x + s.offsetX(), y + s.offsetY());
    }

    public static boolean shown(ScreensModule m) {
        return m != null && m.pencil.get();
    }

    public static void draw(ScreenSession s, GuiGraphics g, int mx, int my) {
        g.pose().pushMatrix();
        g.pose().translate(-s.offsetX(), -s.offsetY());
        try {
            paint(s, g, mx + s.offsetX(), my + s.offsetY());
        } finally {
            g.pose().popMatrix();
        }
    }

    private static void paint(ScreenSession s, GuiGraphics g, float mx, float my) {
        Box b = box(s);
        boolean hot = b.contains(mx, my);
        g.nextStratum();
        Draw.rect(g, b.x(), b.y(), b.w(), b.h(), hot ? Theme.BRAND : 0x8C000000);
        int fg = hot ? 0xFFFFFFFF : 0xB4FFFFFF;
        int x = Math.round(b.x()), y = Math.round(b.y());
        for (int i = 0; i < 6; i++) Draw.rect(g, x + 3 + i, y + 8 - i, 2, 1, fg);
        Draw.rect(g, x + 2, y + 9, 2, 1, hot ? Theme.BRAND2 : 0xFFFFB547);
        if (!hot) return;
        String tip = Lang.tr("screens.ui.pencil", ClientCompat.keyName(ScreensModule.get().editKey.mapping()).getString());
        float tw = Draw.width(tip) + 8;
        float tx = Math.max(1, Math.min(s.width() - tw - 1, b.x() + b.w() / 2f - tw / 2f));
        float ty = b.y() > s.height() / 2f ? b.y() - Draw.CHIP_H : b.bottom() + 2;
        Draw.chip(g, tip, tx, ty, Draw.CHIP_BG, Theme.TEXT);
    }
}
