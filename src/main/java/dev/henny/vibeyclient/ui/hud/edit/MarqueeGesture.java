package dev.henny.vibeyclient.ui.hud.edit;

import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

import java.util.ArrayList;
import java.util.List;

public final class MarqueeGesture {
    private final EditSession s;
    private final List<HudElement> base = new ArrayList<>();
    private final List<HudElement> picked = new ArrayList<>();
    private final HudLayout.Box tmp = new HudLayout.Box();
    private final float x1;
    private final float y1;
    private float x2;
    private float y2;

    MarqueeGesture(EditSession s, double x, double y, boolean add) {
        this.s = s;
        this.x1 = this.x2 = (float) x;
        this.y1 = this.y2 = (float) y;
        if (add) base.addAll(s.selection.items());
        else s.selection.clear();
    }

    void drag(double mx, double my) {
        x2 = (float) mx;
        y2 = (float) my;
        picked.clear();
        picked.addAll(base);
        for (HudElement e : s.canvas()) {
            HudLayout.Box b = s.box(e, tmp);
            boolean in = b.x < right() && b.x + b.w > left() && b.y < bottom() && b.y + b.h > top();
            if (in && !picked.contains(e)) picked.add(e);
        }
        s.selection.set(picked);
    }

    public float left() {
        return Math.min(x1, x2);
    }

    public float top() {
        return Math.min(y1, y2);
    }

    public float right() {
        return Math.max(x1, x2);
    }

    public float bottom() {
        return Math.max(y1, y2);
    }
}
