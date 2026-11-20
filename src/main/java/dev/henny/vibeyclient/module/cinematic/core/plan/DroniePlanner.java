package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.Spline;
import dev.henny.vibeyclient.module.cinematic.core.V3;
import dev.henny.vibeyclient.module.cinematic.core.shot.PathShot;
import dev.henny.vibeyclient.module.cinematic.core.shot.Shot;

import java.util.Random;

public final class DroniePlanner implements Planner {
    @Override
    public Shot plan(Scene s, Random rnd) {
        Tries tries = new Tries(s);
        V3 head = s.feet().add(0, 1.5, 0);
        double far = Math.min(26, s.radius() * 0.65), up = Math.min(16, s.radius() * 0.4);
        for (int i = 0; i < 8 && !tries.exhausted(); i++) {
            double a = Math.toRadians(s.frontAngle() + (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2) * 45 + Tries.range(rnd, -10, 10));
            double scale = i < 4 ? 1 : 0.6;
            V3 out = new V3(Math.sin(a), 0, Math.cos(a)), side = out.side();
            Spline path = Spline.of(
                    s.feet().add(out.mul(2.4)).add(0, 1.6, 0),
                    s.feet().add(out.mul(far * scale * 0.35)).add(side.mul(1.5)).add(0, up * scale * 0.3 + 1.6, 0),
                    s.feet().add(out.mul(far * scale)).add(0, up * scale + 1.6, 0));
            Shot shot = PathShot.along(path, s.time(16)).lookAt(head).fov(62, 72).build();
            if (tries.accept(shot, head)) return shot;
        }
        return null;
    }
}
