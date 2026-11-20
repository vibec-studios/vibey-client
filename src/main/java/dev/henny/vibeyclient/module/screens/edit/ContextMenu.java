package dev.henny.vibeyclient.module.screens.edit;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.screens.apply.Item;
import dev.henny.vibeyclient.module.screens.core.Box;
import dev.henny.vibeyclient.module.screens.core.Placement;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public final class ContextMenu {
    static final int ROW = 12;

    record Entry(String text, Runnable action) {}

    private final List<Entry> entries;
    private final Box box;

    private ContextMenu(List<Entry> entries, double x, double y, int screenW, int screenH) {
        this.entries = entries;
        int w = 0;
        for (Entry e : entries) w = Math.max(w, Draw.width(e.text));
        w += 12;
        int h = entries.size() * ROW + 4;
        float bx = (float) Math.min(x, screenW - w - 2), by = (float) Math.min(y, screenH - h - 2);
        this.box = new Box(Math.max(2, bx), Math.max(2, by), w, h);
    }

    static ContextMenu open(EditSession e, Item item, double x, double y) {
        List<Entry> list = new ArrayList<>();
        if (item != null && !item.panel()) {
            Placement p = EditActions.placement(e, item);
            list.add(new Entry(Lang.tr(p.hidden() ? "screens.ui.show" : "screens.ui.hide"), () -> EditActions.toggleHide(e, item)));
        }
        if (item != null) list.add(new Entry(Lang.tr("screens.ui.resetOne"), () -> EditActions.reset(e, item)));
        if (item == null && e.history.canUndo()) list.add(new Entry(Lang.tr("screens.ui.undo"), () -> EditActions.undo(e)));
        list.add(new Entry(Lang.tr("screens.ui.resetScreen"), () -> EditActions.resetScreen(e)));
        if (item == null) list.add(new Entry(Lang.tr("screens.ui.done"), EditMode::leave));
        return new ContextMenu(list, x, y, e.screen.width(), e.screen.height());
    }

    public List<String> labels() {
        List<String> out = new ArrayList<>();
        for (Entry e : entries) out.add(e.text);
        return out;
    }

    public Box row(int i) {
        return new Box(box.x() + 2, box.y() + 2 + i * ROW, box.w() - 4, ROW);
    }

    Entry hit(double x, double y) {
        for (int i = 0; i < entries.size(); i++) if (row(i).contains(x, y)) return entries.get(i);
        return null;
    }

    void draw(GuiGraphics g, double mx, double my) {
        Draw.rect(g, box.x(), box.y(), box.w(), box.h(), 0xF00B0B0E);
        Draw.outline(g, new Box(box.x() + 1, box.y() + 1, box.w() - 2, box.h() - 2), Theme.LINE2);
        for (int i = 0; i < entries.size(); i++) {
            Box r = row(i);
            if (r.contains(mx, my)) Draw.rect(g, r.x(), r.y(), r.w(), r.h(), Theme.alpha(Theme.BRAND, 0.35f));
            Draw.text(g, entries.get(i).text, r.x() + 4, r.y() + 2, Theme.TEXT);
        }
    }
}
