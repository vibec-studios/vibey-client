package dev.henny.companio.world;

import dev.henny.companio.def.LocoState;
import dev.henny.companio.def.MoveMode;
import dev.henny.companio.def.MovementDef;
import dev.henny.companio.move.Body;

final class Locomotion {
    private Locomotion() {}

    static LocoState of(Body b, MoveMode mode, boolean held, boolean riding, MovementDef m) {
        if (held) return LocoState.HELD;
        if (riding) return LocoState.RIDE;
        double speed = b.speed();
        boolean moving = speed > 0.35;
        if (b.inWater) return speed > 0.4 ? LocoState.SWIM : LocoState.SWIM_IDLE;
        if (mode == MoveMode.FLY && m.canFly() && !b.onGround) return LocoState.FLY;
        if (!b.onGround && b.airTime > 0.3) return LocoState.FALL;
        if (!moving && mode == MoveMode.SLEEP) return LocoState.SLEEP;
        if (!moving && mode == MoveMode.SIT) return LocoState.SIT;
        if (speed > (m.walkSpeed() + m.runSpeed()) / 2) return LocoState.RUN;
        return moving ? LocoState.WALK : LocoState.IDLE;
    }

    static float speedFactor(Body b, LocoState s, MovementDef m) {
        double speed = b.speed();
        return switch (s) {
            case WALK -> (float) Math.max(0.6, Math.min(1.8, speed / m.walkSpeed()));
            case RUN -> (float) Math.max(0.7, Math.min(1.6, speed / m.runSpeed()));
            case SWIM -> (float) Math.max(0.6, Math.min(1.6, speed / Math.max(0.5, m.swimSpeed())));
            default -> 1f;
        };
    }
}
