package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.Clearance;
import dev.henny.vibeyclient.module.cinematic.core.Pose;
import dev.henny.vibeyclient.module.cinematic.core.V3;
import dev.henny.vibeyclient.module.cinematic.core.shot.Shot;

public final class ShotCheck {
    static final double SAMPLE = 0.2;

    private ShotCheck() {}

    public static boolean ok(Scene s, Shot shot) {
        V3 prev = null;
        int n = (int) Math.ceil(shot.duration() / SAMPLE);
        for (int i = 0; i <= n; i++) {
            V3 p = shot.at(Math.min(shot.duration(), i * SAMPLE)).pos();
            if (!inside(s, p) || !Clearance.free(s.world(), p)) return false;
            if (prev != null && !Clearance.segment(s.world(), prev, p)) return false;
            if (i % 2 == 0 && !visible(s, p)) return false;
            prev = p;
        }
        return true;
    }

    public static boolean sees(Scene s, Shot shot, V3 target) {
        double d = shot.duration();
        int hits = 0;
        for (int i = 0; i <= 4; i++) if (Clearance.sight(s.world(), shot.at(d * i / 4).pos(), target)) hits++;
        return hits >= 4;
    }

    public static boolean pose(Scene s, Pose p) {
        return inside(s, p.pos()) && Clearance.free(s.world(), p.pos());
    }

    public static boolean inside(Scene s, V3 p) {
        return p.dist(s.eye()) <= s.radius();
    }

    public static boolean visible(Scene s, V3 p) {
        return Clearance.sight(s.world(), s.eye(), p);
    }
}
