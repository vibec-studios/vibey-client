package dev.henny.vibeyclient.ui.hud.edit;

import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

import java.util.List;

final class MoveGesture {
    private final EditSession s;
    private final AxisSnap snap;
    private final Readouts readouts;
    private final HudLayout.Box start = new HudLayout.Box();
    private final HudLayout.Box group = new HudLayout.Box();
    private final HudLayout.Box tmp = new HudLayout.Box();
    private final HudLayout.State before;
    private final double pressX;
    private final double pressY;
    private final float[] startX;
    private final float[] startY;

    MoveGesture(EditSession s, double pressX, double pressY) {
        this.s = s;
        this.snap = new AxisSnap(s);
        this.readouts = new Readouts(s);
        this.pressX = pressX;
        this.pressY = pressY;
        this.before = HudLayout.capture();
        List<HudElement> sel = s.selection.items();
        startX = new float[sel.size()];
        startY = new float[sel.size()];
        for (int i = 0; i < sel.size(); i++) {
            HudLayout.Box b = s.box(sel.get(i), tmp);
            startX[i] = b.x;
            startY[i] = b.y;
        }
        s.groupBox(start);
    }

    void drag(double mx, double my, boolean free) {
        float gx = (float) (start.x + mx - pressX);
        float gy = (float) (start.y + my - pressY);
        gx = Math.max(0, Math.min(s.width() - start.w, gx));
        gy = Math.max(0, Math.min(s.height() - start.h, gy));
        s.guides.clearLines();
        if (!free) {
            gx = snap.position(true, gx, start.w, gy, start.h);
            gy = snap.position(false, gy, start.h, gx, start.w);
        }
        float ox = gx - start.x, oy = gy - start.y;
        List<HudElement> sel = s.selection.items();
        for (int i = 0; i < sel.size() && i < startX.length; i++) {
            HudLayout.moveTo(sel.get(i), startX[i] + ox, startY[i] + oy, s.width(), s.height());
        }
        s.groupBox(group);
        readouts.measure(group, s.selection.primary());
    }

    void end() {
        s.history.pushIfChanged(before);
        s.guides.clear();
    }
}
