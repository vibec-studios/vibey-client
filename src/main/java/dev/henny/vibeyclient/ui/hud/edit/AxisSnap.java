package dev.henny.vibeyclient.ui.hud.edit;

import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

final class AxisSnap {
    static final float SNAP = 4;
    static final float MARGIN = 4;
    static final float GAP = 2;

    private final EditSession s;
    private final HudLayout.Box tmp = new HudLayout.Box();
    private final float[] targets = new float[7];
    private final float[] lines = new float[7];

    AxisSnap(EditSession s) {
        this.s = s;
    }

    float position(boolean xAxis, float pos, float size, float ox, float ow) {
        float screen = xAxis ? s.width() : s.height();
        float best = pos;
        float bestD = SNAP + 0.5f;
        float guide = Float.NaN, from = 0, to = 0;
        float[] t = targets;
        t[0] = MARGIN; t[1] = 0; t[2] = screen - MARGIN - size; t[3] = screen - size; t[4] = screen / 2f - size / 2f;
        for (int i = 0; i < 5; i++) {
            float d = Math.abs(pos - t[i]);
            if (d >= bestD) continue;
            bestD = d;
            best = t[i];
            guide = i == 4 ? screen / 2f : Float.NaN;
            from = 0;
            to = xAxis ? s.height() : s.width();
        }
        for (HudElement o : s.canvas()) {
            if (s.selection.contains(o)) continue;
            HudLayout.Box b = s.box(o, tmp);
            float a = xAxis ? b.x : b.y, l = xAxis ? b.w : b.h;
            float oa = xAxis ? b.y : b.x, ol = xAxis ? b.h : b.w;
            t[0] = a; t[1] = a + l - size; t[2] = a + l; t[3] = a - size; t[4] = a + l / 2f - size / 2f;
            t[5] = a + l + GAP; t[6] = a - size - GAP;
            float[] g = lines;
            g[0] = a; g[1] = a + l; g[2] = a + l; g[3] = a; g[4] = a + l / 2f; g[5] = a + l + 1; g[6] = a - 1;
            for (int k = 0; k < 7; k++) {
                float d = Math.abs(pos - t[k]);
                if (d >= bestD) continue;
                bestD = d;
                best = t[k];
                guide = g[k];
                from = Math.min(oa, ox) - 4;
                to = Math.max(oa + ol, ox + ow) + 4;
            }
        }
        if (!Float.isNaN(guide)) s.guides.line(xAxis, guide, from, to);
        return best;
    }

    float edge(boolean xAxis, float edge, float fixed, int dir, float ox, float ow) {
        float screen = xAxis ? s.width() : s.height();
        float best = edge, bestD = SNAP + 0.5f, guide = Float.NaN, from = 0, to = 0;
        float[] t = targets;
        t[0] = MARGIN; t[1] = screen - MARGIN; t[2] = screen / 2f; t[3] = 0; t[4] = screen;
        for (int i = 0; i < 5; i++) {
            float d = Math.abs(edge - t[i]);
            if (d >= bestD) continue;
            bestD = d;
            best = t[i];
            guide = i == 2 ? t[i] : Float.NaN;
            from = 0;
            to = xAxis ? s.height() : s.width();
        }
        for (HudElement o : s.canvas()) {
            if (s.selection.contains(o)) continue;
            HudLayout.Box b = s.box(o, tmp);
            float a = xAxis ? b.x : b.y, l = xAxis ? b.w : b.h;
            float oa = xAxis ? b.y : b.x, ol = xAxis ? b.h : b.w;
            t[0] = a; t[1] = a + l; t[2] = a + l / 2f;
            for (int k = 0; k < 3; k++) {
                float d = Math.abs(edge - t[k]);
                if (d >= bestD) continue;
                bestD = d;
                best = t[k];
                guide = t[k];
                from = Math.min(oa, ox) - 4;
                to = Math.max(oa + ol, ox + ow) + 4;
            }
        }
        if (!Float.isNaN(guide)) s.guides.line(xAxis, guide, from, to);
        return Math.max(4, dir > 0 ? best - fixed : fixed - best);
    }
}
