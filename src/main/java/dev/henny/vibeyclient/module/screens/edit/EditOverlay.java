package dev.henny.vibeyclient.module.screens.edit;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.screens.apply.Item;
import dev.henny.vibeyclient.module.screens.core.Box;
import dev.henny.vibeyclient.module.screens.core.Guide;
import dev.henny.vibeyclient.module.screens.core.Hit;
import dev.henny.vibeyclient.module.screens.core.ScaleSteps;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;

public final class EditOverlay {
    private EditOverlay() {}

    public static void draw(EditSession e, GuiGraphics g, int mx, int my) {
        if (e.gesture == null) {
            e.mouseX = mx;
            e.mouseY = my;
        }
        g.nextStratum();
        int w = e.screen.width(), h = e.screen.height();
        Picking.Picked hover = e.gesture == null && e.menu == null ? Picking.at(e, e.mouseX, e.mouseY) : null;
        e.hovered = hover == null ? null : hover.item().id;
        for (Item i : Picking.editable(e)) item(g, e, i, hover);
        for (Guide l : e.guides) guide(g, l, w, h);
        Item sel = e.selectedItem();
        if (sel != null) tag(g, e, sel, true);
        else if (hover != null) tag(g, e, hover.item(), false);
        float ox = e.screen.offsetX(), oy = e.screen.offsetY();
        double rx = e.realX(), ry = e.realY();
        g.pose().pushMatrix();
        g.pose().translate(-ox, -oy);
        try {
            grid(g, w, h, e.gesture != null);
            e.toolbar.place(e, sel);
            if (e.gesture == null || !e.toolbar.bounds().contains(rx, ry)) e.toolbar.draw(g, rx, ry);
            if (sel == null && e.menu == null) hint(g, w, h, e.toolbar.bounds());
            if (e.menu != null) e.menu.draw(g, rx, ry);
            if (EditMode.drawPointer) Draw.pointer(g, rx, ry);
        } finally {
            g.pose().popMatrix();
        }
    }

    private static void grid(GuiGraphics g, int w, int h, boolean busy) {
        int c = busy ? 0x22FFFFFF : 0x10FFFFFF;
        Draw.rect(g, w / 2f, 0, 1, h, c);
        Draw.rect(g, 0, h / 2f, w, 1, c);
    }

    private static void item(GuiGraphics g, EditSession e, Item i, Picking.Picked hover) {
        Box b = i.visual;
        boolean selected = i.id.equals(e.selected);
        boolean hovered = hover != null && hover.item() == i;
        if (i.hidden) {
            Draw.rect(g, b.x(), b.y(), b.w(), b.h(), 0x90141418);
            Draw.dashed(g, b, selected ? Theme.BRAND : 0x60FFFFFF);
            String s = i.label.isBlank() ? Lang.tr("screens.ui.hidden") : i.label;
            if (Draw.width(s) < b.w() - 4) Draw.text(g, s, b.cx() - Draw.width(s) / 2f, b.cy() - 4, Theme.DIM);
            return;
        }
        if (selected) Draw.outline(g, b, Theme.BRAND);
        else if (hovered) Draw.outline(g, b, Theme.alpha(Theme.BRAND, 0.75f));
        else if (i.panel()) Draw.dashed(g, b, 0x80FFFFFF);
        else Draw.outline(g, b, 0x50FFFFFF);
        if ((selected || hovered) && i.resizable) handles(g, b, hovered ? hover.zone() : Hit.Zone.NONE);
    }

    private static void handles(GuiGraphics g, Box b, Hit.Zone hot) {
        int cy = Math.round(b.cy());
        Draw.rect(g, b.x() - 2, cy - 3, 3, 6, hot == Hit.Zone.LEFT ? Theme.BRAND2 : 0xFFFFFFFF);
        Draw.rect(g, b.right() - 1, cy - 3, 3, 6, hot == Hit.Zone.RIGHT ? Theme.BRAND2 : 0xFFFFFFFF);
    }

    private static void guide(GuiGraphics g, Guide l, int w, int h) {
        float from = Math.max(0, l.from()), to = Math.min(l.vertical() ? h : w, l.to());
        if (l.vertical()) Draw.rect(g, Math.min(w - 1, l.at()), from, 1, Math.max(1, to - from), Theme.BRAND2);
        else Draw.rect(g, from, Math.min(h - 1, l.at()), Math.max(1, to - from), 1, Theme.BRAND2);
    }

    private static void tag(GuiGraphics g, EditSession e, Item i, boolean selected) {
        String name = i.panel() ? i.label : shortLabel(i.label);
        String s = name + (i.scalable || i.panel() ? "  " + ScaleSteps.percent(scale(e, i)) + "%" : "");
        if (i.hidden) s = s + "  " + Lang.tr("screens.ui.hidden");
        float tw = Draw.width(s) + 8;
        float ty = i.visual.y() - 13 >= 0 ? i.visual.y() - 13 : i.visual.bottom() + 2;
        float tx = Math.max(1, Math.min(e.screen.width() - tw - 1, i.visual.x() - 1));
        Draw.chip(g, s, tx, ty, selected ? Theme.BRAND : Draw.CHIP_BG, Theme.TEXT);
    }

    private static float scale(EditSession e, Item i) {
        return i.panel() ? e.screen.layout().scale() : e.screen.layout().placement(i.id).scale();
    }

    private static String shortLabel(String s) {
        if (s == null || s.isBlank()) return Lang.tr("screens.ui.widget");
        return s.length() > 24 ? s.substring(0, 23) + "…" : s;
    }

    private static void hint(GuiGraphics g, int w, int h, Box toolbar) {
        String s = Lang.tr("screens.ui.hint");
        float tw = Draw.width(s) + 8;
        if (toolbar == null) return;
        float y = toolbar.y() > h / 2f ? toolbar.y() - Draw.CHIP_H : toolbar.bottom() + 1;
        Draw.chip(g, s, w / 2f - tw / 2f, y, 0xB4000000, Theme.MUTED);
    }
}
