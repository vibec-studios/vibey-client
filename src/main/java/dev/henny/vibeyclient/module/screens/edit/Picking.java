package dev.henny.vibeyclient.module.screens.edit;

import dev.henny.vibeyclient.module.screens.apply.Item;
import dev.henny.vibeyclient.module.screens.core.Box;
import dev.henny.vibeyclient.module.screens.core.Hit;

import java.util.ArrayList;
import java.util.List;

final class Picking {
    record Picked(Item item, Hit.Zone zone) {}

    private Picking() {}

    static List<Item> editable(EditSession e) {
        List<Item> out = new ArrayList<>();
        if (e.screen.panel() != null) out.add(e.screen.panel());
        for (Item i : e.screen.items()) {
            if (i.shown() && i.visual.w() >= 2 && i.visual.h() >= 2) out.add(i);
        }
        return out;
    }

    static Picked at(EditSession e, double x, double y) {
        List<Item> items = editable(e);
        List<Box> boxes = new ArrayList<>(items.size());
        List<Boolean> resizable = new ArrayList<>(items.size());
        for (Item i : items) {
            boxes.add(i.visual);
            resizable.add(i.resizable && !i.hidden);
        }
        Hit.Result r = Hit.find(boxes, resizable, x, y);
        if (r.index() < 0) return null;
        Item sel = e.selectedItem();
        if (sel != null && !sel.panel() && sel != items.get(r.index()) && sel.visual.contains(x, y)) {
            int i = items.indexOf(sel);
            if (i < 0) for (int k = 0; k < items.size(); k++) if (items.get(k).id.equals(sel.id)) i = k;
            if (i >= 0) {
                Hit.Result own = Hit.find(List.of(boxes.get(i)), List.of(resizable.get(i)), x, y);
                return new Picked(items.get(i), own.zone());
            }
        }
        return new Picked(items.get(r.index()), r.zone());
    }

    static List<Box> others(EditSession e, Item moving) {
        List<Box> out = new ArrayList<>();
        Box panel = moving.panel() ? moving.visual : null;
        for (Item i : editable(e)) {
            if (i == moving || i.id.equals(moving.id) || i.hidden) continue;
            if (panel != null && panel.containsCenterOf(i.visual)) continue;
            out.add(i.visual);
        }
        return out;
    }
}
