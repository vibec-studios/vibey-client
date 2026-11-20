package dev.henny.vibeyclient.dev;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.vui.menu.MenuScreen;
import dev.henny.vibeyclient.ui.vui.menu.ModulePage;
import dev.henny.vui.dom.Element;
import dev.henny.vui.geom.Rect;
import dev.henny.vui.layout.ScreenRects;

final class MenuTargets {
    private MenuTargets() {}

    static double[] center(String id, double dx) {
        if (!(ClientCompat.screen() instanceof MenuScreen s)) return new double[] {0, 0};
        s.document().layoutIfNeeded();
        Element e = s.document().find(id);
        if (e == null) return new double[] {0, 0};
        Rect r = ScreenRects.of(e);
        return new double[] {dx >= 0 ? r.x() + dx : r.centerX(), r.centerY()};
    }

    static String panel() {
        return ClientCompat.screen() instanceof MenuScreen s ? s.controller().panel.peek() : null;
    }

    static String page() {
        if (!(ClientCompat.screen() instanceof MenuScreen s)) return null;
        ModulePage p = s.controller().page.peek();
        return p == null ? null : p.getId();
    }
}
