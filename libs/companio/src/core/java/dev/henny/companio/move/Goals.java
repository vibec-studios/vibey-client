package dev.henny.companio.move;

import dev.henny.companio.def.MovementDef;
import dev.henny.companio.def.MoveMode;
import dev.henny.companio.sense.Owner;
import dev.henny.companio.sense.Senses;

import java.util.Random;

final class Goals {
    private final MovementDef m;
    private final Random random;
    private boolean following, nearSet, wanderSet, swimSet;
    private double nearX, nearY, nearZ, wanderX, wanderY, wanderZ, swimX, swimY, swimZ, nextWander, nextSwim, angle;

    Goals(MovementDef m, Random random) {
        this.m = m;
        this.random = random;
    }

    void pick(MoveMode mode, Senses s, Body b, Terrain t, Home home, double now, float dt, Goal g) {
        Owner o = s.owner;
        g.none();
        if (mode != MoveMode.NEAR) nearSet = false;
        switch (mode) {
            case FOLLOW, HOP, FLY -> follow(o, b, g);
            case NEAR -> near(o, b, t, g);
            case WANDER -> wander(home, o, b, t, now, g);
            case SWIM -> swim(s, b, t, now, g);
            case FLEE -> flee(s, b, t, g);
            case CIRCLE -> circle(o, dt, g);
            case SHOULDER, HEAD -> {
                if (o.present) g.set(o.x, o.y, o.z, 1.1, m.runSpeed());
            }
            default -> following = false;
        }
    }

    private void follow(Owner o, Body b, Goal g) {
        if (!o.present) return;
        double d = b.flatDistance(o.x, o.z) + Math.abs(o.y - b.y) * 0.5;
        var f = m.follow();
        if (!following && d > f.far()) following = true;
        if (following && d < f.near() + 0.25) following = false;
        if (!following) return;
        boolean run = d > f.run() || (o.sprinting && d > f.far()) || o.speed > m.walkSpeed() * 1.2;
        g.set(o.x, o.y, o.z, f.near(), run ? m.runSpeed() : m.walkSpeed());
    }

    private void near(Owner o, Body b, Terrain t, Goal g) {
        if (!o.present) return;
        if (!nearSet || Math.hypot(nearX - o.x, nearZ - o.z) > m.follow().far()) {
            double yaw = Math.toRadians(o.bodyYaw);
            double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
            double px = o.x + rx * m.follow().near() * 0.8, pz = o.z + rz * m.follow().near() * 0.8;
            int[] spot = t.standable((int) Math.floor(px), (int) Math.floor(o.y + 0.01), (int) Math.floor(pz))
                    ? new int[] {(int) Math.floor(px), (int) Math.floor(o.y + 0.01), (int) Math.floor(pz)}
                    : t.spotNear(px, o.y, pz, 2);
            if (spot == null) spot = new int[] {(int) Math.floor(o.x), (int) Math.floor(o.y), (int) Math.floor(o.z)};
            nearX = spot[0] + 0.5;
            nearY = spot[1];
            nearZ = spot[2] + 0.5;
            nearSet = true;
        }
        double d = b.flatDistance(nearX, nearZ);
        g.set(nearX, nearY, nearZ, 0.3, d > m.follow().run() ? m.runSpeed() : m.walkSpeed());
    }

    private void wander(Home home, Owner o, Body b, Terrain t, double now, Goal g) {
        double cx = home.set ? home.x : o.x, cy = home.set ? home.y : o.y, cz = home.set ? home.z : o.z;
        if (!wanderSet || now > nextWander || b.flatDistance(wanderX, wanderZ) < 0.4) {
            nextWander = now + 4 + random.nextDouble() * 6;
            double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * m.wander();
            int[] spot = t.spotNear(cx + Math.cos(a) * r, cy, cz + Math.sin(a) * r, 2);
            wanderSet = spot != null;
            if (spot != null) {
                wanderX = spot[0] + 0.5;
                wanderY = spot[1];
                wanderZ = spot[2] + 0.5;
            }
            if (random.nextFloat() < 0.35) wanderSet = false;
        }
        if (wanderSet) g.set(wanderX, wanderY, wanderZ, 0.3, m.walkSpeed() * 0.55);
    }

    private void swim(Senses s, Body b, Terrain t, double now, Goal g) {
        if (!m.canSwim()) {
            follow(s.owner, b, g);
            return;
        }
        if (!swimSet || now > nextSwim || b.flatDistance(swimX, swimZ) < 0.6) {
            nextSwim = now + 3 + random.nextDouble() * 4;
            int[] w = Water.pick(t.view(), b, b.inWater ? 5 : 8, random);
            swimSet = w != null;
            if (w != null) {
                swimX = w[0] + 0.5;
                swimY = w[1];
                swimZ = w[2] + 0.5;
            }
        }
        if (swimSet) g.set(swimX, swimY, swimZ, 0.5, b.inWater ? m.swimSpeed() : m.walkSpeed());
        else follow(s.owner, b, g);
    }

    private void flee(Senses s, Body b, Terrain t, Goal g) {
        double fx = s.danger ? s.dangerX : s.owner.x, fz = s.danger ? s.dangerZ : s.owner.z;
        double dx = b.x - fx, dz = b.z - fz, len = Math.max(0.01, Math.hypot(dx, dz));
        double gx = b.x + dx / len * 6, gz = b.z + dz / len * 6;
        int[] spot = t.spotNear(gx, b.y, gz, 2);
        if (spot != null) g.set(spot[0] + 0.5, spot[1], spot[2] + 0.5, 0.5, m.runSpeed());
        else g.set(gx, b.y, gz, 0.5, m.runSpeed());
    }

    private void circle(Owner o, float dt, Goal g) {
        if (!o.present) return;
        angle += dt * m.runSpeed() / 2.2;
        g.set(o.x + Math.cos(angle) * 2.2, o.y, o.z + Math.sin(angle) * 2.2, 0.2, m.runSpeed());
        g.direct = true;
    }
}
