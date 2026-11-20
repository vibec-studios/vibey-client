package dev.henny.cosmetico.sim;

import dev.henny.cosmetico.state.PetMotion;

final class PetSteer {
    private PetSteer() {}

    static void follow(PetMotion pm, PetInput p, boolean ground, double half, double tx, double ty, double tz, double rx, double rz) {
        double k = ground ? 0.18 : 0.25;
        double dx = (tx - pm.x) * k, dz = (tz - pm.z) * k;
        pm.x += dx;
        pm.z += dz;
        if (ground) {
            double ox = pm.x - p.x(), oz = pm.z - p.z();
            double d = Math.sqrt(ox * ox + oz * oz), min = 0.3 + half + 0.08;
            if (d < min) {
                if (d < 1e-4) {
                    ox = rx;
                    oz = rz;
                    d = 1;
                }
                double push = Math.min(min - d, 0.25);
                pm.x += ox / d * push;
                pm.z += oz / d * push;
                dx += ox / d * push;
                dz += oz / d * push;
            }
        }
        pm.y += (ty - pm.y) * (ground ? 0.35 : 0.2);
        double hs = Math.sqrt(dx * dx + dz * dz);
        pm.move = Approach.to(pm.move, (float) Math.min(1.0, hs * 9.0), 0.3f);
        float targetYaw = hs > 0.01 ? (float) Math.toDegrees(Math.atan2(-dx, dz)) : p.bodyYaw();
        float d = ((targetYaw - pm.yaw) % 360f + 540f) % 360f - 180f;
        pm.yaw += d * (hs > 0.01 ? 0.35f : 0.08f);
    }
}
