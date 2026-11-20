package dev.henny.vibeyclient.module.screens.edit;

import dev.henny.vibeyclient.module.screens.apply.Item;
import dev.henny.vibeyclient.module.screens.apply.ScreenSession;
import dev.henny.vibeyclient.module.screens.core.Guide;
import dev.henny.vibeyclient.module.screens.core.History;
import dev.henny.vibeyclient.module.screens.core.ScreenLayout;

import java.util.ArrayList;
import java.util.List;

public final class EditSession {
    final ScreenSession screen;
    final History<ScreenLayout> history = new History<>();
    final List<Guide> guides = new ArrayList<>();
    final Toolbar toolbar = new Toolbar();
    String selected;
    String hovered;
    Gesture gesture;
    ContextMenu menu;
    double mouseX = -1;
    double mouseY = -1;

    EditSession(ScreenSession screen) {
        this.screen = screen;
    }

    double realX() {
        return mouseX + screen.offsetX();
    }

    double realY() {
        return mouseY + screen.offsetY();
    }

    public ScreenSession screen() {
        return screen;
    }

    public String selected() {
        return selected;
    }

    public Item selectedItem() {
        return screen.item(selected);
    }

    public boolean menuOpen() {
        return menu != null;
    }

    public Toolbar toolbar() {
        return toolbar;
    }

    public List<Guide> guides() {
        return guides;
    }
}
