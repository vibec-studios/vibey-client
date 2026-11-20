package dev.henny.companio.move;

import java.util.List;

final class Steering {
    private List<int[]> path = List.of();
    private int index;
    private double goalX, goalY, goalZ, plannedAt = -1e9;
    private boolean planned;

    void reset() {
        path = List.of();
        index = 0;
        planned = false;
    }

    boolean steer(Body b, Terrain t, double gx, double gy, double gz, double stopWithin, double speed, boolean canJump,
                  double now, MoveIntent out) {
        double d = b.flatDistance(gx, gz);
        if (d <= stopWithin && Math.abs(gy - b.y) < 2.5) {
            reset();
            return false;
        }
        boolean direct = d < 3.5 && Math.abs(gy - b.y) < 0.6 && !b.blocked;
        if (direct) {
            reset();
            toward(b, gx, gz, speed, d - stopWithin, out);
            return true;
        }
        if (!planned || now - plannedAt > 1.5 || dist(goalX, goalZ, gx, gz) > 1.5 || index >= path.size()) {
            plan(b, t, gx, gy, gz, stopWithin, canJump, now);
        }
        if (index >= path.size()) {
            toward(b, gx, gz, speed, d - stopWithin, out);
            return true;
        }
        int[] n = path.get(index);
        double nx = n[0] + 0.5, nz = n[2] + 0.5;
        if (b.flatDistance(nx, nz) < 0.35 && Math.abs(n[1] - b.y) < 1.1) {
            index++;
            if (index >= path.size()) return true;
            n = path.get(index);
            nx = n[0] + 0.5;
            nz = n[2] + 0.5;
        }
        toward(b, nx, nz, speed, index == path.size() - 1 ? d - stopWithin : 9, out);
        if (n[1] > b.y + 0.55 && canJump) out.jump = true;
        if (b.blocked && b.onGround && canJump) out.jump = true;
        return true;
    }

    private void plan(Body b, Terrain t, double gx, double gy, double gz, double stopWithin, boolean canJump, double now) {
        planned = true;
        plannedAt = now;
        goalX = gx;
        goalY = gy;
        goalZ = gz;
        int sx = Collide.floor(b.x), sy = Collide.floor(b.y + 0.2), sz = Collide.floor(b.z);
        path = PathFinder.find(t, sx, sy, sz, Collide.floor(gx), Collide.floor(gy + 0.2), Collide.floor(gz), Math.max(0.5, stopWithin), canJump);
        index = 0;
    }

    static void toward(Body b, double tx, double tz, double speed, double remaining, MoveIntent out) {
        double dx = tx - b.x, dz = tz - b.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-4) return;
        double s = speed / 20.0 * Math.max(0.25, Math.min(1.0, remaining / 0.9));
        out.vx = dx / len * s;
        out.vz = dz / len * s;
    }

    private static double dist(double ax, double az, double bx, double bz) {
        double dx = ax - bx, dz = az - bz;
        return Math.sqrt(dx * dx + dz * dz);
    }

    boolean hasPath() {
        return index < path.size();
    }
}
