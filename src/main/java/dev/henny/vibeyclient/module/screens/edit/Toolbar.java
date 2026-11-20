package dev.henny.vibeyclient.module.screens.edit;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.screens.apply.Item;
import dev.henny.vibeyclient.module.screens.core.Box;
import dev.henny.vibeyclient.module.screens.core.ScaleSteps;
import dev.henny.vibeyclient.module.screens.core.ScreenLayout;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public final class Toolbar {
    public enum Kind { SCALE, DIM, BLUR, TITLE, RESET, DONE }

    record Chip(Kind kind, String text, Box box, boolean stepper, boolean on) {}

    static final int ARROW = 9;
    static final int GAP = 3;

    private final List<Chip> chips = new ArrayList<>();

    public List<Chip> chips() {
        return chips;
    }

    public Box box(Kind kind) {
        for (Chip c : chips) if (c.kind == kind) return c.box;
        return null;
    }

    void place(EditSession e, Item sel) {
        if (e.gesture != null && !chips.isEmpty()) {
            Box b = bounds();
            double my = e.realY();
            boolean under = my < b.bottom() + 6 && b.y() < e.screen.height() / 2f
                    || my > b.y() - 6 && b.y() > e.screen.height() / 2f;
            if (!under) return;
            layout(e, b.y() < e.screen.height() / 2f);
            return;
        }
        layout(e, false);
        int top = overlaps(e, sel);
        layout(e, true);
        int bottom = overlaps(e, sel);
        if (top <= bottom) layout(e, false);
    }

    private int overlaps(EditSession e, Item sel) {
        Box b = bounds();
        int n = 0;
        for (Item i : Picking.editable(e)) {
            if (i.panel()) continue;
            Box v = i.visual;
            if (v.x() < b.right() && b.x() < v.right() && v.y() < b.bottom() && b.y() < v.bottom()) n += i == sel ? 50 : 1;
        }
        return n;
    }

    void layout(EditSession e, boolean bottom) {
        ScreenLayout l = e.screen.layout();
        chips.clear();
        List<Object[]> specs = List.of(
                new Object[] {Kind.SCALE, Lang.tr("screens.ui.size") + " " + ScaleSteps.percent(l.scale()) + "%", true, false},
                new Object[] {Kind.DIM, Lang.tr("screens.ui.dim") + " " + value(l.dim(), "%"), true, false},
                new Object[] {Kind.BLUR, Lang.tr("screens.ui.blur") + " " + value(l.blur(), ""), true, false},
                new Object[] {Kind.TITLE, Lang.tr("screens.ui.title"), false, !l.hideTitle()},
                new Object[] {Kind.RESET, Lang.tr("screens.ui.reset"), false, false},
                new Object[] {Kind.DONE, Lang.tr("screens.ui.done"), false, false});
        float total = -GAP;
        float[] widths = new float[specs.size()];
        for (int i = 0; i < specs.size(); i++) {
            Object[] s = specs.get(i);
            widths[i] = Draw.width((String) s[1]) + 8 + ((Boolean) s[2] ? ARROW * 2 : 0);
            total += widths[i] + GAP;
        }
        float x = Math.max(2, e.screen.width() / 2f - total / 2f);
        float y = bottom ? e.screen.height() - Draw.CHIP_H - 3 : 3;
        for (int i = 0; i < specs.size(); i++) {
            Object[] s = specs.get(i);
            chips.add(new Chip((Kind) s[0], (String) s[1], new Box(x, y, widths[i], Draw.CHIP_H), (Boolean) s[2], (Boolean) s[3]));
            x += widths[i] + GAP;
        }
    }

    private static String value(int v, String unit) {
        return v == ScreenLayout.VANILLA ? Lang.tr("screens.ui.auto") : v + unit;
    }

    Box bounds() {
        if (chips.isEmpty()) return null;
        Box a = chips.get(0).box, b = chips.get(chips.size() - 1).box;
        return new Box(a.x() - 2, a.y() - 2, b.right() - a.x() + 4, a.h() + 4);
    }

    boolean press(EditSession e, double x, double y, int button) {
        for (Chip c : chips) {
            if (!c.box.contains(x, y)) continue;
            int dir = button == 1 || (c.stepper && x < c.box.x() + ARROW) ? -1 : 1;
            run(e, c.kind, dir);
            return true;
        }
        return false;
    }

    boolean wheel(EditSession e, double x, double y, int dir) {
        for (Chip c : chips) {
            if (c.box.contains(x, y) && c.stepper) {
                run(e, c.kind, dir);
                return true;
            }
        }
        return false;
    }

    private static void run(EditSession e, Kind k, int dir) {
        switch (k) {
            case SCALE -> EditActions.screenScale(e, dir);
            case DIM -> EditActions.dim(e, dir);
            case BLUR -> EditActions.blur(e, dir);
            case TITLE -> EditActions.title(e);
            case RESET -> EditActions.resetScreen(e);
            case DONE -> EditMode.leave();
        }
    }

    void draw(GuiGraphics g, double mx, double my) {
        Box all = bounds();
        if (all == null) return;
        Draw.rect(g, all.x(), all.y(), all.w(), all.h(), 0xC8000000);
        for (Chip c : chips) {
            boolean hot = c.box.contains(mx, my);
            int bg = c.kind == Kind.DONE ? (hot ? Theme.BRAND_HOVER : Theme.BRAND) : (hot ? Theme.SURFACE3 : Theme.SURFACE2);
            Draw.rect(g, c.box.x(), c.box.y(), c.box.w(), c.box.h(), bg);
            int fg = c.kind == Kind.TITLE && !c.on ? Theme.DIM : Theme.TEXT;
            float tx = c.box.x() + 4 + (c.stepper ? ARROW : 0);
            Draw.text(g, c.text, tx, c.box.y() + 3, fg);
            if (c.kind == Kind.TITLE && !c.on) Draw.rect(g, tx - 1, c.box.y() + 6, Draw.width(c.text) + 2, 1, Theme.DIM);
            if (!c.stepper) continue;
            Draw.text(g, "-", c.box.x() + 3, c.box.y() + 3, Theme.MUTED);
            Draw.text(g, "+", c.box.right() - ARROW + 1, c.box.y() + 3, Theme.MUTED);
        }
    }
}
