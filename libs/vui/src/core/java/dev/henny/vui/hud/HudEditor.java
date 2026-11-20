package dev.henny.vui.hud;

import dev.henny.vui.geom.Rect;

import java.util.ArrayList;
import java.util.List;

public final class HudEditor {
    public static final float SCALE_STEP = 0.1f;

    private final HudLayer layer;
    private final List<SnapGuide> guides = new ArrayList<>();
    private boolean active;
    private HudWidget selected;
    private HudWidget hovered;
    private boolean dragging;
    private float grabX;
    private float grabY;
    private boolean snapping = true;

    HudEditor(HudLayer layer) {
        this.layer = layer;
    }

    public boolean active() {
        return active;
    }

    public void active(boolean on) {
        active = on;
        if (!on) {
            dragging = false;
            guides.clear();
        }
    }

    public HudWidget selected() {
        return selected;
    }

    public HudWidget hovered() {
        return hovered;
    }

    public List<SnapGuide> guides() {
        return guides;
    }

    public boolean dragging() {
        return dragging;
    }

    public void snapping(boolean on) {
        snapping = on;
    }

    public boolean pointerMove(float x, float y) {
        if (!active) return false;
        hovered = layer.widgetAt(x, y);
        if (!dragging || selected == null) return hovered != null;
        Rect current = selected.bounds();
        Rect moved = new Rect(x - grabX, y - grabY, current.width(), current.height());
        guides.clear();
        if (snapping) {
            SnapEngine.Result r = SnapEngine.snap(moved, others(selected), layer.width(), layer.height(), SnapEngine.THRESHOLD);
            moved = r.rect();
            guides.addAll(r.guides());
        }
        moved = moved.clampInside(layer.width(), layer.height());
        place(selected, moved);
        return true;
    }

    public boolean pointerDown(float x, float y) {
        if (!active) return false;
        HudWidget hit = layer.widgetAt(x, y);
        selected = hit;
        if (hit == null) return false;
        dragging = true;
        grabX = x - hit.bounds().x();
        grabY = y - hit.bounds().y();
        return true;
    }

    public boolean pointerUp(float x, float y) {
        if (!active) return false;
        boolean was = dragging;
        dragging = false;
        guides.clear();
        return was;
    }

    public boolean scroll(float x, float y, float dy) {
        if (!active || dy == 0) return false;
        HudWidget target = layer.widgetAt(x, y);
        if (target == null) target = selected;
        if (target == null) return false;
        Rect before = target.bounds();
        HudPlacement scaled = target.placement().withScale(target.placement().scale() + Math.signum(dy) * SCALE_STEP);
        float w = target.contentWidth() * scaled.scale();
        float h = target.contentHeight() * scaled.scale();
        Rect resized = new Rect(before.centerX() - w / 2f, before.centerY() - h / 2f, w, h).clampInside(layer.width(), layer.height());
        target.placement(HudPlacement.fromRect(resized, scaled.scale(), layer.width(), layer.height(), scaled.visible()));
        layer.layout();
        return true;
    }

    public boolean nudge(float dx, float dy) {
        if (!active || selected == null) return false;
        Rect r = selected.bounds();
        place(selected, new Rect(r.x() + dx, r.y() + dy, r.width(), r.height()).clampInside(layer.width(), layer.height()));
        return true;
    }

    public void reset(HudWidget w) {
        w.placement(w.defaults());
        layer.layout();
    }

    public void toggleVisible(HudWidget w) {
        w.placement(w.placement().withVisible(!w.visible()));
    }

    private void place(HudWidget w, Rect rect) {
        w.placement(HudPlacement.fromRect(rect, w.placement().scale(), layer.width(), layer.height(), w.visible()));
        layer.layout();
    }

    private List<Rect> others(HudWidget except) {
        List<Rect> out = new ArrayList<>();
        for (HudWidget w : layer.widgets()) if (w != except && w.visible()) out.add(w.bounds());
        return out;
    }
}
