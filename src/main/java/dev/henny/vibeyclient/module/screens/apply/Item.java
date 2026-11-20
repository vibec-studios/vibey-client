package dev.henny.vibeyclient.module.screens.apply;

import dev.henny.vibeyclient.module.screens.core.Box;
import net.minecraft.client.gui.components.AbstractWidget;

public final class Item {
    public static final String PANEL = "panel";

    public final String id;
    public final String fallback;
    public final AbstractWidget widget;
    public final boolean scalable;
    public final boolean resizable;
    public final Box base;
    public final Box visual;
    public final boolean hidden;
    public final String label;

    Item(String id, String fallback, AbstractWidget widget, boolean scalable, boolean resizable, Box base, Box visual,
         boolean hidden, String label) {
        this.id = id;
        this.fallback = fallback;
        this.widget = widget;
        this.scalable = scalable;
        this.resizable = resizable;
        this.base = base;
        this.visual = visual;
        this.hidden = hidden;
        this.label = label;
    }

    public boolean panel() {
        return widget == null;
    }

    public boolean shown() {
        return hidden || widget == null || widget.visible;
    }
}
