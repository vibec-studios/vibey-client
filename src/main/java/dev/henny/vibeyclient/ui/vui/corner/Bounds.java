package dev.henny.vibeyclient.ui.vui.corner;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;

import java.util.List;

record Bounds(int minX, int minY, int maxX, int maxY) {
    static Bounds of(List<? extends GuiEventListener> widgets, int fallbackY) {
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;
        for (GuiEventListener l : widgets) {
            if (!shown(l)) continue;
            LayoutElement e = (LayoutElement) l;
            x0 = Math.min(x0, e.getX());
            y0 = Math.min(y0, e.getY());
            x1 = Math.max(x1, e.getX() + e.getWidth());
            y1 = Math.max(y1, e.getY() + e.getHeight());
        }
        if (y0 == Integer.MAX_VALUE) return new Bounds(0, fallbackY, 0, fallbackY);
        return new Bounds(x0, y0, x1, y1);
    }

    static boolean free(List<? extends GuiEventListener> widgets, int x, int y, int w, int h) {
        if (x < 0 || y < 0) return false;
        for (GuiEventListener l : widgets) {
            if (!shown(l)) continue;
            LayoutElement e = (LayoutElement) l;
            if (e.getX() < x + w + 2 && e.getX() + e.getWidth() > x - 2 && e.getY() < y + h + 2 && e.getY() + e.getHeight() > y - 2) return false;
        }
        return true;
    }

    private static boolean shown(GuiEventListener l) {
        if (!(l instanceof LayoutElement e) || l instanceof ProxyButton) return false;
        if (l instanceof AbstractWidget w && !w.visible) return false;
        return e.getWidth() > 0 && e.getHeight() > 0;
    }
}
