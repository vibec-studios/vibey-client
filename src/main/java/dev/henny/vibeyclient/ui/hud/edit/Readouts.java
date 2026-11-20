package dev.henny.vibeyclient.ui.hud.edit;

import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

final class Readouts {
    static final float NEAR = 48;

    private final EditSession s;
    private final HudLayout.Box tmp = new HudLayout.Box();

    Readouts(EditSession s) {
        this.s = s;
    }

    void measure(HudLayout.Box b, HudElement anchorOf) {
        Guides g = s.guides;
        g.clearReadouts();
        int sw = s.width(), sh = s.height();
        Anchor a = HudLayout.placement(anchorOf).anchor;
        float cy = b.y + b.h / 2f, cx = b.x + b.w / 2f;
        if (a.col == 0) g.readout(0, cy, b.x, cy, b.x);
        else if (a.col == 2) g.readout(b.x + b.w, cy, sw, cy, sw - b.x - b.w);
        else g.readout(sw / 2f, cy, cx, cy, cx - sw / 2f);
        if (a.row == 0) g.readout(cx, 0, cx, b.y, b.y);
        else if (a.row == 2) g.readout(cx, b.y + b.h, cx, sh, sh - b.y - b.h);
        else g.readout(cx, sh / 2f, cx, cy, cy - sh / 2f);
        g.anchor(a.fx() * sw, a.fy() * sh);
        neighbours(b, cx, cy);
    }

    private void neighbours(HudLayout.Box b, float cx, float cy) {
        float bestL = NEAR, bestR = NEAR, bestU = NEAR, bestD = NEAR;
        float lX = 0, rX = 0, uY = 0, dY = 0;
        for (HudElement o : s.canvas()) {
            if (s.selection.contains(o)) continue;
            HudLayout.Box ob = s.box(o, tmp);
            if (ob.y < b.y + b.h && ob.y + ob.h > b.y) {
                float dl = b.x - (ob.x + ob.w);
                if (dl >= 0 && dl < bestL) { bestL = dl; lX = ob.x + ob.w; }
                float dr = ob.x - (b.x + b.w);
                if (dr >= 0 && dr < bestR) { bestR = dr; rX = ob.x; }
            }
            if (ob.x < b.x + b.w && ob.x + ob.w > b.x) {
                float du = b.y - (ob.y + ob.h);
                if (du >= 0 && du < bestU) { bestU = du; uY = ob.y + ob.h; }
                float dd = ob.y - (b.y + b.h);
                if (dd >= 0 && dd < bestD) { bestD = dd; dY = ob.y; }
            }
        }
        Guides g = s.guides;
        if (bestL < NEAR) g.readout(lX, cy + 3, b.x, cy + 3, bestL);
        if (bestR < NEAR) g.readout(b.x + b.w, cy + 3, rX, cy + 3, bestR);
        if (bestU < NEAR) g.readout(cx + 3, uY, cx + 3, b.y, bestU);
        if (bestD < NEAR) g.readout(cx + 3, b.y + b.h, cx + 3, dY, bestD);
    }
}
