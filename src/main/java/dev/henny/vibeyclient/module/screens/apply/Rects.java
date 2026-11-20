package dev.henny.vibeyclient.module.screens.apply;

import dev.henny.vibeyclient.module.screens.core.Box;
import net.minecraft.client.gui.components.AbstractWidget;

final class Rects {
    private Rects() {}

    static Box of(AbstractWidget w) {
        return new Box(w.getX(), w.getY(), w.getWidth(), w.getHeight());
    }

    static void set(AbstractWidget w, Box b) {
        w.setX(Math.round(b.x()));
        w.setY(Math.round(b.y()));
        w.setWidth(Math.max(1, Math.round(b.w())));
        w.setHeight(Math.max(1, Math.round(b.h())));
    }

    static WidgetState state(AbstractWidget w, boolean create) {
        StateHolder h = (StateHolder) w;
        WidgetState s = h.vibey$screens();
        if (s == null && create) {
            s = new WidgetState();
            h.vibey$screens(s);
        }
        return s;
    }
}
