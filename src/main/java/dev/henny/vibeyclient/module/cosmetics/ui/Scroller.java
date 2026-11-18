package dev.henny.vibeyclient.module.cosmetics.ui;

import dev.henny.vibeyclient.ui.Ui;

import dev.henny.vibeyclient.ui.Anim;

/**
 * {@code ScrollArea} for {@link Ui}: {@code int y = s.begin(ui, x, y, w, h); ...rows...; s.end(ui, y);}
 * Clips its content, takes the mouse wheel while hovered, scrolls smoothly and draws a thin scrollbar.
 */
final class Scroller {
    private float target;
    private final Anim smooth = new Anim(0f, 120);
    private int contentHeight;
    private int x, y, w, h;
    private int start;

    int begin(Ui ui, int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        double wheel = ui.takeScroll(x, y, w, h);
        if (wheel != 0) target -= (float) (wheel * 28);
        clampTarget();
        float off = smooth.to(target);
        ui.clip(x, y, x + w, y + h);
        start = y - Math.round(off);
        return start;
    }

    void end(Ui ui, int endY) {
        contentHeight = endY - start;
        ui.unclip();
        int max = maxScroll();
        if (max > 0) {
            float off = smooth.value();
            int thumb = Math.max(16, h * h / Math.max(1, contentHeight));
            int ty = y + Math.round((h - thumb) * Math.min(1f, off / max));
            ui.g.rect(x + w - 2, ty, 2, thumb, ui.hover(x, y, w, h) ? 0x50FFFFFF : 0x28FFFFFF);
        }
    }

    void reset() {
        target = 0;
        smooth.snap(0);
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - h);
    }

    private void clampTarget() {
        target = Math.max(0, Math.min(target, maxScroll()));
    }
}
