package dev.henny.companio.move;

import dev.henny.companio.def.MovementDef;
import dev.henny.companio.sense.Owner;

final class Separation {
    static final double SECONDS = 3;

    private double since = -1;

    boolean cutOff(MovementDef m, Body b, Owner o, double now) {
        boolean apart = o.present && o.onGround && !o.flying && b.onGround && !b.inWater
                && Math.abs(o.y - b.y) > m.jump() + 1.4 && b.flatDistance(o.x, o.z) < m.follow().run();
        if (!apart) {
            since = -1;
            return false;
        }
        if (since < 0) since = now;
        if (now - since < SECONDS) return false;
        since = -1;
        return true;
    }
}
