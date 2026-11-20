package dev.henny.companio.move;

public final class Physics {
    static final double GRAVITY = 0.08, DRAG = 0.98, MAX_FALL = 3.0;

    private Physics() {}

    public static void step(Body b, BlockView v, MoveIntent in, float stepHeight) {
        if (in.frozen) {
            b.vx = b.vy = b.vz = 0;
            return;
        }
        fluids(b, v);
        double accel = b.inWater ? 0.25 : b.onGround ? (b.ground == Surface.ICE ? 0.08 : 0.45) : 0.12;
        b.vx += (in.vx - b.vx) * accel;
        b.vz += (in.vz - b.vz) * accel;
        vertical(b, in);
        if (in.jump && (b.onGround || (b.inWater && nearSurface(b)))) b.vy = jumpVelocity(in.jumpHeight);
        moveY(b, v);
        moveH(b, v, stepHeight);
        if (b.onGround) {
            b.airTime = 0;
            b.ground = v.surface(Collide.floor(b.x), Collide.floor(b.y - 0.05), Collide.floor(b.z));
        } else b.airTime += 0.05;
    }

    static double jumpVelocity(float height) {
        return Math.sqrt(2 * GRAVITY * Math.max(0.2, height)) + 0.04;
    }

    private static void vertical(Body b, MoveIntent in) {
        if (in.fly) {
            b.vy += (in.vy - b.vy) * 0.2;
            return;
        }
        if (b.inWater && in.swim) {
            double target = b.waterTop - b.height * 0.6;
            b.vy += Math.max(-0.04, Math.min(0.05, (target - b.y) * 0.12));
            b.vy *= 0.8;
            return;
        }
        b.vy -= b.inWater ? GRAVITY * 0.25 : GRAVITY;
        b.vy *= b.inWater ? 0.8 : DRAG;
        if (b.vy < -MAX_FALL) b.vy = -MAX_FALL;
    }

    private static boolean nearSurface(Body b) {
        return !Double.isNaN(b.waterTop) && b.waterTop - b.y < b.height;
    }

    private static void fluids(Body b, BlockView v) {
        int x = Collide.floor(b.x), z = Collide.floor(b.z);
        int y = Collide.floor(b.y + 0.1);
        Fluid f = v.fluid(x, y, z);
        b.inLava = f == Fluid.LAVA;
        b.inWater = f == Fluid.WATER || v.fluid(x, Collide.floor(b.y + b.height * 0.5), z) == Fluid.WATER;
        if (!b.inWater) {
            b.waterTop = Double.NaN;
            return;
        }
        int top = y;
        while (top < y + 8 && v.fluid(x, top + 1, z) == Fluid.WATER) top++;
        b.waterTop = top + 0.9;
    }

    private static void moveY(Body b, BlockView v) {
        double dy = b.vy, half = b.width / 2.0;
        if (dy <= 0) {
            double target = b.y + dy;
            double g = Collide.ground(v, b.x, b.y, target, b.z, half);
            if (g > Double.NEGATIVE_INFINITY) {
                land(b, g);
            } else {
                b.y = target;
                b.onGround = false;
            }
        } else {
            double ny = b.y + dy;
            if (Collide.blocked(v, b.x, ny, b.z, half, b.height)) b.vy = 0;
            else b.y = ny;
            b.onGround = false;
        }
        if (!b.onGround && b.y > b.fallFrom) b.fallFrom = b.y;
    }

    private static void land(Body b, double g) {
        b.y = g;
        b.vy = 0;
        b.onGround = true;
    }

    private static void moveH(Body b, BlockView v, float step) {
        b.blocked = false;
        double half = b.width / 2.0;
        double nx = b.x + b.vx;
        if (!tryMove(b, v, nx, b.z, half, step)) {
            b.vx = 0;
            b.blocked = true;
        }
        double nz = b.z + b.vz;
        if (!tryMove(b, v, b.x, nz, half, step)) {
            b.vz = 0;
            b.blocked = true;
        }
    }

    private static boolean tryMove(Body b, BlockView v, double nx, double nz, double half, float step) {
        if (!Collide.blocked(v, nx, b.y, nz, half, b.height)) {
            b.x = nx;
            b.z = nz;
            return true;
        }
        if (!b.onGround && !b.inWater) return false;
        double top = Collide.obstacleTop(v, nx, b.y, nz, half, b.height);
        if (top - b.y > step + Collide.EPS) return false;
        if (Collide.blocked(v, nx, top, nz, half, b.height)) return false;
        b.x = nx;
        b.z = nz;
        b.y = top;
        return true;
    }
}
