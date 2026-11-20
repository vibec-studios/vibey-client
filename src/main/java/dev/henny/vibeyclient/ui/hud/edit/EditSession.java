package dev.henny.vibeyclient.ui.hud.edit;

import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class EditSession {
    public final Selection selection = new Selection();
    public final History history = new History();
    public final Guides guides = new Guides();
    public final HudLayout.State opened = HudLayout.capture();
    private final List<HudElement> registered = new ArrayList<>();
    private final List<HudElement> available = new ArrayList<>();
    private final List<HudElement> canvas = new ArrayList<>();
    private final HudLayout.Box tmp = new HudLayout.Box();
    private BiConsumer<String, Boolean> toasts = (s, undo) -> {};
    private boolean snapping = true;
    private int width = 1;
    private int height = 1;

    public void onToast(BiConsumer<String, Boolean> sink) {
        toasts = sink;
    }

    void toast(String text, boolean undoable) {
        toasts.accept(text, undoable);
    }

    public void refresh(int sw, int sh) {
        width = Math.max(1, sw);
        height = Math.max(1, sh);
        HudLayout.elements(registered);
        available.clear();
        canvas.clear();
        for (HudElement e : registered) {
            if (!e.available()) continue;
            available.add(e);
            if (!HudLayout.hidden(e)) canvas.add(e);
        }
        selection.prune(e -> !canvas.contains(e) || HudLayout.get(e.id()) != e);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public List<HudElement> available() {
        return available;
    }

    public List<HudElement> canvas() {
        return canvas;
    }

    public boolean snapping() {
        return snapping;
    }

    public void snapping(boolean on) {
        snapping = on;
    }

    public HudLayout.Box box(HudElement e, HudLayout.Box out) {
        return HudLayout.box(e, width, height, out);
    }

    public HudLayout.Box groupBox(HudLayout.Box out) {
        float x1 = Float.MAX_VALUE, y1 = Float.MAX_VALUE, x2 = -Float.MAX_VALUE, y2 = -Float.MAX_VALUE;
        for (HudElement e : selection.items()) {
            HudLayout.Box b = box(e, tmp);
            x1 = Math.min(x1, b.x);
            y1 = Math.min(y1, b.y);
            x2 = Math.max(x2, b.x + b.w);
            y2 = Math.max(y2, b.y + b.h);
        }
        if (x1 == Float.MAX_VALUE) x1 = y1 = x2 = y2 = 0;
        out.x = x1;
        out.y = y1;
        out.w = x2 - x1;
        out.h = y2 - y1;
        out.scale = 1;
        return out;
    }

    public HudElement elementAt(double mx, double my) {
        List<HudElement> sel = selection.items();
        for (int i = sel.size() - 1; i >= 0; i--) {
            HudLayout.Box b = box(sel.get(i), tmp);
            if (mx >= b.x - 2 && mx < b.x + b.w + 2 && my >= b.y - 2 && my < b.y + b.h + 2) return sel.get(i);
        }
        for (int i = canvas.size() - 1; i >= 0; i--) {
            HudLayout.Box b = box(canvas.get(i), tmp);
            if (mx >= b.x && mx < b.x + b.w && my >= b.y && my < b.y + b.h) return canvas.get(i);
        }
        return null;
    }

    public EditActions actions() {
        return new EditActions(this);
    }
}
