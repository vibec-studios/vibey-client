package dev.henny.vibeyclient.ui;

import dev.henny.vibeyclient.api.GlobalSettings;

/**
 * Vertical scroll list: {@code int y = area.begin(ui, x, y, w, h); ...draw rows, advancing y...; area.end(ui, y);}
 * Clips its content, takes the mouse wheel while hovered and glides to the target with frame-rate independent
 * smoothing (repeated wheel ticks add up without restarting an animation, so it never hitches). The thin
 * scrollbar can be dragged. Keep one instance per list (a field of the panel).
 */
public final class ScrollArea {
    /** Time constant of the glide in seconds (≈ 140 ms to settle). */
    private static final float TAU = 0.035f;

    private float target;
    private float value;
    private long lastNanos;
    private int contentHeight;
    private int x, y, w, h;
    private int start;
    private float dragOffset = Float.NaN;

    public int begin(Ui ui, int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        double wheel = ui.takeScroll(x, y, w, h);
        if (wheel != 0) target -= (float) (wheel * 28);
        clampTarget();
        step();
        ui.clip(x, y, x + w, y + h);
        start = y - Math.round(value);
        return start;
    }

    public void end(Ui ui, int endY) {
        contentHeight = endY - start;
        ui.unclip();
        int max = maxScroll();
        if (max > 0) {
            int thumb = Math.max(16, h * h / Math.max(1, contentHeight));
            int ty = y + Math.round((h - thumb) * Math.min(1f, value / max));
            int bx = x + w - 4;
            boolean dragging = ui.dragging(this, bx - 2, ty, 6, thumb);
            if (dragging) {
                if (Float.isNaN(dragOffset)) dragOffset = ui.mouseY - ty;
                float f = (ui.mouseY - dragOffset - y) / Math.max(1f, h - thumb);
                target = Math.max(0, Math.min(1, f)) * max;
                value = target;
            } else {
                dragOffset = Float.NaN;
            }
            boolean hot = dragging || ui.hover(x, y, w, h);
            ui.g.rect(x + w - 3, ty, 2, thumb, hot ? 0x5CFFFFFF : 0x24FFFFFF);
        } else {
            target = 0;
            value = 0;
        }
    }

    private void step() {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0 : Math.min(0.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        if (!GlobalSettings.animate()) {
            value = target;
            return;
        }
        float k = 1f - (float) Math.exp(-dt / TAU);
        value += (target - value) * k;
        if (Math.abs(target - value) < 0.25f) value = target;
    }

    public void scrollTo(int offset) {
        target = offset;
        clampTarget();
    }

    /** Scrolls the least amount so that content rows {@code top..bottom} (content coordinates) are visible. */
    public void reveal(int top, int bottom) {
        if (top < target) target = top;
        else if (bottom > target + h) target = bottom - h;
        clampTarget();
    }

    public void reset() {
        target = 0;
        value = 0;
    }

    public int offset() {
        return Math.round(value);
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - h);
    }

    private void clampTarget() {
        target = Math.max(0, Math.min(target, maxScroll()));
    }
}
