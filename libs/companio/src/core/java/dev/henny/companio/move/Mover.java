package dev.henny.companio.move;

import dev.henny.companio.def.MoveMode;
import dev.henny.companio.def.MovementDef;
import dev.henny.companio.sense.Owner;
import dev.henny.companio.sense.Senses;

import java.util.Random;

public final class Mover {
    private static final double STUCK_SECONDS = 3;

    private final MovementDef m;
    private final Goals goals;
    private final Goal goal = new Goal();
    private final Steering steering = new Steering();
    private final Separation separation = new Separation();
    public final Home home = new Home();
    private MoveMode riding;
    private double stuckSince = -1, lastHop;
    private boolean teleported, mounted, dismounted;

    public Mover(MovementDef m, Random random) {
        this.m = m;
        this.goals = new Goals(m, random);
    }

    public void tick(MoveMode mode, Senses s, Body b, Terrain t, MoveIntent out, double now, float dt) {
        out.clear();
        teleported = mounted = dismounted = false;
        out.jumpHeight = m.jump();
        Owner o = s.owner;
        if (riding != null && mode != riding) dismount(b, t, o);
        if (riding != null) {
            out.frozen = true;
            return;
        }
        if (!t.view().loaded(Collide.floor(b.x), Collide.floor(b.z))) {
            if (follows(mode) && o.present) teleport(b, t, o);
            else out.frozen = true;
            return;
        }
        if (follows(mode) && o.present && b.distance(o.x, o.y, o.z) > m.follow().teleport() && !o.elytra) {
            teleport(b, t, o);
            return;
        }
        goals.pick(mode, s, b, t, home, now, dt, goal);
        out.swim = b.inWater && m.canSwim();
        if (mode.riding() && o.present && b.distance(o.x, o.y, o.z) < 1.6) {
            riding = mode;
            mounted = true;
            out.frozen = true;
            return;
        }
        boolean moving = goal.active && move(mode, b, t, out, now);
        stuck(mode, b, t, o, moving, now);
        if (mode == MoveMode.HOP && b.onGround && now - lastHop > 0.55) {
            out.jump = true;
            out.jumpHeight = 0.6f;
            lastHop = now;
        }
        if (b.inLava || b.inWater && !m.canSwim()) out.jump = true;
    }

    private boolean move(MoveMode mode, Body b, Terrain t, MoveIntent out, double now) {
        if (mode == MoveMode.FLY && m.canFly()) {
            out.fly = true;
            Steering.toward(b, goal.x, goal.z, m.flySpeed(), b.flatDistance(goal.x, goal.z) - goal.stopWithin, out);
            out.vy = Math.max(-0.2, Math.min(0.2, (goal.y + 1.4 - b.y) * 0.08));
            return b.flatDistance(goal.x, goal.z) > goal.stopWithin;
        }
        if (goal.direct) {
            Steering.toward(b, goal.x, goal.z, goal.speed, 9, out);
            if (b.blocked && b.onGround) out.jump = true;
            return true;
        }
        double speed = b.inWater && m.canSwim() ? Math.max(m.swimSpeed(), 0.5) : goal.speed;
        return steering.steer(b, t, goal.x, goal.y, goal.z, goal.stopWithin, speed, m.jump() >= 0.9f, now, out);
    }

    private void stuck(MoveMode mode, Body b, Terrain t, Owner o, boolean moving, double now) {
        if (follows(mode) && separation.cutOff(m, b, o, now)) {
            teleport(b, t, o);
            stuckSince = -1;
            return;
        }
        boolean progress = b.speed() > 0.25 || !moving;
        if (progress) {
            stuckSince = -1;
            return;
        }
        if (stuckSince < 0) stuckSince = now;
        if (now - stuckSince > STUCK_SECONDS && follows(mode) && o.present && b.distance(o.x, o.y, o.z) > m.follow().far()) {
            teleport(b, t, o);
            stuckSince = -1;
        }
    }

    public void teleport(Body b, Terrain t, Owner o) {
        int[] spot = t.spotNear(o.x, o.y, o.z, 3);
        if (spot != null) b.place(spot[0] + 0.5, spot[1], spot[2] + 0.5);
        else b.place(o.x, o.y, o.z);
        steering.reset();
        teleported = true;
    }

    private void dismount(Body b, Terrain t, Owner o) {
        riding = null;
        dismounted = true;
        if (o.present) teleport(b, t, o);
        teleported = false;
    }

    public void forceDismount(Body b, Terrain t, Owner o) {
        if (riding != null) dismount(b, t, o);
    }

    private static boolean follows(MoveMode mode) {
        return switch (mode) {
            case FOLLOW, NEAR, HOP, FLY, SWIM, CIRCLE, SHOULDER, HEAD -> true;
            default -> false;
        };
    }

    public MoveMode riding() {
        return riding;
    }

    public boolean teleported() {
        return teleported;
    }

    public boolean mounted() {
        return mounted;
    }

    public boolean dismounted() {
        return dismounted;
    }

    public boolean wantsToMove() {
        return goal.active;
    }
}
