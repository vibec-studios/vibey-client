package dev.henny.cosmetico.sim;

import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.PetFollow;
import dev.henny.cosmetico.state.PetMotion;

public final class PetFollower {
    private PetFollower() {}

    public static void step(PetMotion pm, Bundle b, PetInput p, long tick) {
        pm.keepPrevious();
        if (b == null) {
            pm.init = false;
            return;
        }
        PetFollow follow = b.pet == null ? PetFollow.GROUND : b.pet.follow();
        boolean ground = follow == PetFollow.GROUND;
        double a = Math.toRadians(p.bodyYaw());
        double half = PetGeometry.halfWidth(b);
        double side = ground ? PetGeometry.side(b) : 0.7, back = ground ? 0.35 + Math.max(0, half - 0.3) * 0.5 : 0.25;
        double rx = -Math.cos(a), rz = -Math.sin(a);
        double fx = -Math.sin(a), fz = Math.cos(a);
        double tx = p.x() + rx * side - fx * back;
        double tz = p.z() + rz * side - fz * back;
        double ty = p.y() + (ground ? 0 : 1.55 + Math.sin(tick * 0.08) * 0.06);
        if (!pm.init || pm.x == 0 && pm.z == 0 || dist2(pm.x, pm.z, tx, tz) > 12 * 12) {
            pm.place(tx, ty, tz, p.bodyYaw());
            pm.init = true;
            pm.move = 0;
            return;
        }
        PetSteer.follow(pm, p, ground, half, tx, ty, tz, rx, rz);
    }

    private static double dist2(double ax, double az, double bx, double bz) {
        return (ax - bx) * (ax - bx) + (az - bz) * (az - bz);
    }
}
