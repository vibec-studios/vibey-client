package dev.henny.vibeyclient.ui.hud.editor;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.hud.HudElement;

import java.util.ArrayList;
import java.util.List;

final class ContextMenu {
    private ContextMenu() {}

    static void open(EditorController c, float x, float y, HudElement hit) {
        if (hit != null && !c.session.selection.contains(hit)) c.session.selection.only(hit);
        List<Items.MenuEntry> items = new ArrayList<>();
        if (hit != null) {
            boolean copies = true, dup = false;
            for (HudElement e : c.session.selection.items()) {
                copies &= e.isCopy();
                dup |= e.duplicable();
            }
            items.add(entry("style", "sliders", "ui.hud.style", ""));
            if (dup) items.add(entry("duplicate", "copy", "ui.hud.duplicate", "Ctrl+D"));
            items.add(copies ? entry("hide", "hud_trash", "ui.hud.remove", "Del") : entry("hide", "eye", "ui.hud.hide", "H"));
            items.add(entry("reset", "reset", "ui.hud.reset", "R"));
        } else {
            items.add(entry("widgets", "plus", "ui.hud.widgets", ""));
            items.add(entry("all", "layers", "ui.hud.selectAll", "Ctrl+A"));
            items.add(entry("global", "sliders", "ui.hud.allWidgets", ""));
            if (c.session.history.canUndo()) items.add(entry("undo", "undo", "ui.hud.undo", "Ctrl+Z"));
        }
        c.menu.set(new Items.Menu(x, y, items));
        c.panel.set("menu");
    }

    private static Items.MenuEntry entry(String id, String icon, String key, String keys) {
        return new Items.MenuEntry(id, icon, Lang.tr(key), keys);
    }

    static void pick(EditorController c, String id) {
        switch (id) {
            case "style" -> {
                c.style.scope("one");
                c.panel.set("style");
            }
            case "duplicate" -> c.actions.duplicateSelection();
            case "hide" -> c.actions.hideSelection();
            case "reset" -> c.actions.resetSelection();
            case "widgets" -> c.panel.set("widgets");
            case "all" -> c.actions.selectAll();
            case "global" -> {
                if (c.session.selection.isEmpty() && !c.session.canvas().isEmpty()) c.session.selection.only(c.session.canvas().get(0));
                c.style.scope("all");
                c.panel.set("style");
            }
            case "undo" -> c.actions.undo();
            default -> {
            }
        }
    }
}
