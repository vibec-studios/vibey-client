package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.Clearance;
import dev.henny.vibeyclient.module.cinematic.core.Pose;
import dev.henny.vibeyclient.module.cinematic.core.V3;
import dev.henny.vibeyclient.module.cinematic.core.shot.Shot;

final class Tries {
    static final int BUDGET = 28;

    static final double ROOMY = 0.8, CLOSE = 0.45;

    private final Scene scene;
    private final double margin;
    private final double view;
    private int left = BUDGET;

    Tries(Scene scene) {
        this(scene, ROOMY, 3);
    }

    Tries(Scene scene, double margin, double view) {
        this.scene = scene;
        this.margin = margin;
        this.view = view;
    }

    boolean exhausted() {
        return left <= 0;
    }

    boolean accept(Shot shot, V3 target) {
        if (shot == null || left-- <= 0) return false;
        if (!quick(shot) || !roomy(shot)) return false;
        if (target != null && !ShotCheck.sees(scene, shot, target)) return false;
        return ShotCheck.ok(scene, shot);
    }

    private boolean quick(Shot shot) {
        double d = shot.duration();
        for (int i = 0; i <= 2; i++) {
            V3 p = shot.at(d * i / 2).pos();
            if (!ShotCheck.inside(scene, p) || !Clearance.free(scene.world(), p) || !ShotCheck.visible(scene, p)) return false;
        }
        return true;
    }

    private boolean roomy(Shot shot) {
        double d = shot.duration();
        for (int i = 0; i <= 6; i++) {
            Pose p = shot.at(d * i / 6);
            if (!Clearance.free(scene.world(), p.pos(), margin)) return false;
            if (view > 0 && !Clearance.openView(scene.world(), p, view)) return false;
        }
        return true;
    }

    static double range(java.util.Random r, double lo, double hi) {
        return lo + r.nextDouble() * (hi - lo);
    }
}
