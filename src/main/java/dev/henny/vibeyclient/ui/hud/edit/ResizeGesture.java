package dev.henny.vibeyclient.ui.hud.edit;

import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

final class ResizeGesture {
    private final EditSession s;
    private final HudElement e;
    private final int handle;
    private final AxisSnap snap;
    private final HudLayout.Box start = new HudLayout.Box();
    private final HudLayout.Box tmp = new HudLayout.Box();
    private final HudLayout.State before;
    private final double pressX;
    private final double pressY;
    private final float startScale;
    private int chipKey = Integer.MIN_VALUE;

    ResizeGesture(EditSession s, HudElement e, int handle, double pressX, double pressY) {
        this.s = s;
        this.e = e;
        this.handle = handle;
        this.snap = new AxisSnap(s);
        this.pressX = pressX;
        this.pressY = pressY;
        this.before = HudLayout.capture();
        s.box(e, start);
        startScale = start.scale;
    }

    int handle() {
        return handle;
    }

    void drag(double mx, double my, boolean free, boolean shape) {
        s.guides.clearLines();
        int sx = Handles.HX[handle], sy = Handles.HY[handle];
        float dmx = (float) (mx - pressX), dmy = (float) (my - pressY);
        if (shape && e.isWidget()) freeSize(sx, sy, dmx, dmy, !free);
        else proportional(sx, sy, dmx, dmy, !free);
        chip();
    }

    private void freeSize(int sx, int sy, float dmx, float dmy, boolean snapOn) {
        float right = start.x + start.w, bottom = start.y + start.h;
        float nw = start.w + sx * dmx, nh = start.h + sy * dmy;
        if (snapOn && sx != 0) nw = snap.edge(true, sx > 0 ? start.x + nw : right - nw, sx > 0 ? start.x : right, sx, start.y, start.h);
        if (snapOn && sy != 0) nh = snap.edge(false, sy > 0 ? start.y + nh : bottom - nh, sy > 0 ? start.y : bottom, sy, start.x, start.w);
        HudLayout.Placement p = HudLayout.placement(e);
        int bw = sx == 0 ? p.bw : Math.max(4, Math.round(nw / startScale));
        int bh = sy == 0 ? p.bh : Math.max(4, Math.round(nh / startScale));
        HudLayout.setSize(e, bw, bh);
        float w = HudLayout.boxWidth(e) * startScale, h = HudLayout.boxHeight(e) * startScale;
        HudLayout.moveTo(e, sx < 0 ? right - w : start.x, sy < 0 ? bottom - h : start.y, s.width(), s.height());
    }

    private void proportional(int sx, int sy, float dmx, float dmy, boolean snapOn) {
        float right = start.x + start.w, bottom = start.y + start.h;
        float f;
        if (sx != 0 && sy != 0) {
            float fx = (start.w + sx * dmx) / start.w, fy = (start.h + sy * dmy) / start.h;
            f = (fx * start.w * start.w + fy * start.h * start.h) / (start.w * start.w + start.h * start.h);
        } else if (sx != 0) {
            f = (start.w + sx * dmx) / start.w;
        } else {
            f = (start.h + sy * dmy) / start.h;
        }
        float scale = HudLayout.clampScale(e, startScale * f);
        if (snapOn) scale = snapScale(sx, sy, scale, right, bottom);
        HudLayout.placement(e).scale = scale;
        HudLayout.markCustom(e);
        float w = HudLayout.boxWidth(e) * scale, h = HudLayout.boxHeight(e) * scale;
        float x = sx < 0 ? right - w : sx > 0 ? start.x : start.x + start.w / 2f - w / 2f;
        float y = sy < 0 ? bottom - h : sy > 0 ? start.y : start.y + start.h / 2f - h / 2f;
        HudLayout.moveTo(e, x, y, s.width(), s.height());
    }

    private float snapScale(int sx, int sy, float scale, float right, float bottom) {
        float w = start.w * scale / startScale, h = start.h * scale / startScale;
        if (sx != 0) {
            float nw = snap.edge(true, sx > 0 ? start.x + w : right - w, sx > 0 ? start.x : right, sx, start.y, h);
            return nw == w ? scale : clamp(startScale * nw / start.w);
        }
        float nh = snap.edge(false, sy > 0 ? start.y + h : bottom - h, sy > 0 ? start.y : bottom, sy, start.x, w);
        return nh == h ? scale : clamp(startScale * nh / start.h);
    }

    private float clamp(float v) {
        return Math.max(e.minScale(), Math.min(e.maxScale(), v));
    }

    private void chip() {
        HudLayout.Box b = s.box(e, tmp);
        int key = Math.round(b.w) * 100_000 + Math.round(b.h) * 100 + Math.round(b.scale * 100) % 100;
        if (key == chipKey) return;
        chipKey = key;
        s.guides.sizeChip(Math.round(b.w / b.scale) + " × " + Math.round(b.h / b.scale) + "  " + ScaleSteps.percent(b.scale) + "%");
    }

    void end() {
        s.history.pushIfChanged(before);
        s.guides.clear();
    }
}
