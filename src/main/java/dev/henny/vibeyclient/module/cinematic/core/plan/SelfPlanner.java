package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.Spline;
import dev.henny.vibeyclient.module.cinematic.core.V3;
import dev.henny.vibeyclient.module.cinematic.core.shot.OrbitShot;
import dev.henny.vibeyclient.module.cinematic.core.shot.PathShot;
import dev.henny.vibeyclient.module.cinematic.core.shot.Shot;

import java.util.Random;

public final class SelfPlanner implements Planner {
    static final double LOOK = 1.25;
    private int next;

    @Override
    public Shot plan(Scene s, Random rnd) {
        Tries tries = new Tries(s);
        V3 target = s.feet().add(0, LOOK, 0);
        for (int i = 0; i < 9 && !tries.exhausted(); i++) {
            int kind = next + i;
            double angle = s.frontAngle() + (i / 3) * 50 * (rnd.nextBoolean() ? 1 : -1) + Tries.range(rnd, -20, 20);
            Shot shot = style(s, angle, rnd, kind);
            if (tries.accept(shot, target)) {
                next = kind + 1;
                return shot;
            }
        }
        return null;
    }

    static Shot style(Scene s, double angle, Random r, int kind) {
        V3 c = s.feet();
        int dir = r.nextBoolean() ? 1 : -1;
        return switch (Math.floorMod(kind, 3)) {
            case 0 -> OrbitShot.around(c, Tries.range(r, 3.4, 4.4), Tries.range(r, 0.9, 1.6),
                    angle - dir * 80, angle + dir * Tries.range(r, 70, 110), LOOK, s.time(18), 55);
            case 1 -> OrbitShot.around(c, 2.9, 0.45, angle - dir * 35, angle + dir * 35, LOOK + 0.2, s.time(13), 62)
                    .drift(2.6, 0.8);
            default -> {
                V3 out = new V3(Math.sin(Math.toRadians(angle)), 0, Math.cos(Math.toRadians(angle)));
                yield PathShot.along(Spline.of(c.add(out.mul(5.2)).add(0, 1.9, 0), c.add(out.mul(2.3)).add(0, 1.5, 0)), s.time(12))
                        .lookAt(c.add(0, 1.45, 0)).fov(58, 50).build();
            }
        };
    }
}
