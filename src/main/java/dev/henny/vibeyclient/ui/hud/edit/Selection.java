package dev.henny.vibeyclient.ui.hud.edit;

import dev.henny.vibeyclient.ui.hud.HudElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class Selection {
    private final List<HudElement> items = new ArrayList<>();
    private int version;

    public List<HudElement> items() {
        return items;
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int version() {
        return version;
    }

    public HudElement primary() {
        return items.isEmpty() ? null : items.get(items.size() - 1);
    }

    public boolean contains(HudElement e) {
        for (int i = 0; i < items.size(); i++) if (items.get(i) == e) return true;
        return false;
    }

    public void only(HudElement e) {
        items.clear();
        if (e != null) items.add(e);
        version++;
    }

    public void add(HudElement e) {
        if (e == null || contains(e)) return;
        items.add(e);
        version++;
    }

    public void remove(HudElement e) {
        if (items.remove(e)) version++;
    }

    public void toggle(HudElement e) {
        if (contains(e)) remove(e);
        else add(e);
    }

    public void set(List<HudElement> list) {
        items.clear();
        items.addAll(list);
        version++;
    }

    public void clear() {
        if (items.isEmpty()) return;
        items.clear();
        version++;
    }

    public void prune(Predicate<HudElement> gone) {
        if (items.removeIf(gone)) version++;
    }
}
