package dev.henny.vibeyclient.module.screens.apply;

import dev.henny.vibeyclient.module.screens.Screens;
import dev.henny.vibeyclient.module.screens.core.Box;
import dev.henny.vibeyclient.module.screens.core.ScreenLayout;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public final class ScreenSession {
    public final Screen screen;
    public final String name;
    final List<Item> items = new ArrayList<>();
    final WidgetState panelState = new WidgetState();
    Item panel;
    boolean broken;
    float scale = 1f;

    ScreenSession(Screen screen) {
        this.screen = screen;
        this.name = screen.getClass().getName();
    }

    public ScreenLayout layout() {
        return Screens.store().get(name);
    }

    public List<Item> items() {
        return items;
    }

    public Item panel() {
        return panel;
    }

    public Item item(String id) {
        if (id == null) return null;
        if (panel != null && panel.id.equals(id)) return panel;
        for (Item i : items) if (i.id.equals(id)) return i;
        return null;
    }

    public List<Box> visibleBoxes(String except) {
        List<Box> out = new ArrayList<>();
        if (panel != null && !panel.id.equals(except)) out.add(panel.visual);
        for (Item i : items) if (!i.id.equals(except) && !i.hidden && i.shown()) out.add(i.visual);
        return out;
    }

    public boolean broken() {
        return broken;
    }

    public float scale() {
        return scale;
    }

    public void scale(float k) {
        scale = k;
    }

    public int width() {
        return screen.width;
    }

    public int height() {
        return screen.height;
    }
}
