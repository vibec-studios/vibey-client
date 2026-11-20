package dev.henny.vibeyclient.module.visuals.items.edit;

import dev.henny.vibeyclient.module.visuals.items.core.EditSession;
import dev.henny.vibeyclient.module.visuals.items.core.HandPick;
import dev.henny.vibeyclient.module.visuals.items.core.PoseGestures;

final class EditGestures {
    enum Mode { MOVE, TURN, ROLL }

    private final ItemEditController c;
    private Mode mode;
    private double lastX, lastY;

    EditGestures(ItemEditController c) {
        this.c = c;
    }

    boolean dragging() {
        return mode != null;
    }

    void grab(double x, double y, int mainSide, float w, float h, boolean ctrl, boolean alt) {
        c.pickHand(HandPick.nearest(c.session, mainSide, x, y, w, h) == EditSession.OFF ? "off" : "main");
        mode = alt ? Mode.ROLL : ctrl ? Mode.TURN : Mode.MOVE;
        lastX = x;
        lastY = y;
        c.grabbing.set(true);
    }

    void drag(double x, double y, int side, float h) {
        if (mode == null) return;
        float dx = (float) (x - lastX), dy = (float) (y - lastY);
        lastX = x;
        lastY = y;
        if (dx == 0 && dy == 0) return;
        switch (mode) {
            case MOVE -> c.session.edit(p -> PoseGestures.move(p, side, dx, dy, h));
            case TURN -> c.session.edit(p -> PoseGestures.turn(p, side, dx, dy));
            case ROLL -> c.session.edit(p -> PoseGestures.roll(p, side, dx));
        }
    }

    void release() {
        mode = null;
        c.grabbing.set(false);
    }

    void wheel(double x, double y, int mainSide, float w, float h, double steps, boolean depth) {
        if (steps == 0) return;
        if (mode == null) c.pickHand(HandPick.nearest(c.session, mainSide, x, y, w, h) == EditSession.OFF ? "off" : "main");
        c.session.edit(p -> depth ? PoseGestures.wheelDepth(p, steps) : PoseGestures.wheelScale(p, steps));
    }
}
