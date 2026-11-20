package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.Spline;
import dev.henny.vibeyclient.module.cinematic.core.V3;
import dev.henny.vibeyclient.module.cinematic.core.shot.OrbitShot;
import dev.henny.vibeyclient.module.cinematic.core.shot.PathShot;
import dev.henny.vibeyclient.module.cinematic.core.shot.Shot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class WaypointsPlanner implements Planner {
    private int next;

    @Override
    public boolean possible(Scene s) {
        return !near(s).isEmpty();
    }

    List<Scene.Mark> near(Scene s) {
        List<Scene.Mark> out = new ArrayList<>();
        for (Scene.Mark m : s.marks()) if (m.pos().dist(s.eye()) <= s.radius() + 8) out.add(m);
        return out;
    }

    @Override
    public Shot plan(Scene s, Random rnd) {
        List<Scene.Mark> marks = near(s);
        Tries tries = new Tries(s);
        for (int k = 0; k < marks.size() && !tries.exhausted(); k++) {
            Scene.Mark m = marks.get(Math.floorMod(next + k, marks.size()));
            V3 target = m.pos().add(0, 1, 0);
            for (int i = 0; i < 5 && !tries.exhausted(); i++) {
                Shot shot = style(s, m, target, rnd, i);
                if (shot != null && tries.accept(shot, target)) {
                    next = next + k + 1;
                    return shot;
                }
            }
        }
        return null;
    }

    static Shot style(Scene s, Scene.Mark m, V3 target, Random r, int kind) {
        if (kind % 2 == 0) {
            V3 toward = target.sub(s.eye());
            double len = toward.length();
            double stop = Math.min(len - 5, s.radius() - 3);
            if (stop < 4) return null;
            V3 dir = toward.norm(), side = dir.side().mul(Tries.range(r, -2, 2));
            V3 a = s.eye().add(0, 2.5, 0), b = s.eye().add(dir.mul(stop * 0.5)).add(side).add(0, 4, 0), c = s.eye().add(dir.mul(stop)).add(0, 3, 0);
            return PathShot.along(Spline.of(a, b, c), s.time(14)).lookAt(target).fov(65, 55).subject(m.name()).build();
        }
        double angle = r.nextDouble() * 360;
        OrbitShot o = OrbitShot.around(m.pos(), Tries.range(r, 5, 7), Tries.range(r, 2.5, 4), angle, angle + 80, 1, s.time(15), 60);
        return new OrbitShot(o.center(), o.radius0(), o.radius1(), o.height0(), o.height1(), o.fromDeg(), o.toDeg(),
                o.lookHeight(), o.duration(), o.fov(), m.name(), false);
    }
}
