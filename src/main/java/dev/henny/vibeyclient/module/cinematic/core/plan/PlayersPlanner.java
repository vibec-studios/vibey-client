package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.Spline;
import dev.henny.vibeyclient.module.cinematic.core.V3;
import dev.henny.vibeyclient.module.cinematic.core.shot.OrbitShot;
import dev.henny.vibeyclient.module.cinematic.core.shot.PathShot;
import dev.henny.vibeyclient.module.cinematic.core.shot.Shot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class PlayersPlanner implements Planner {
    private String last = "";

    @Override
    public boolean possible(Scene s) {
        return !candidates(s).isEmpty();
    }

    List<Scene.Subject> candidates(Scene s) {
        List<Scene.Subject> out = new ArrayList<>();
        for (Scene.Subject p : s.players()) {
            V3 head = p.head();
            if (head.dist(s.eye()) <= s.radius() - 2 && ShotCheck.visible(s, head)) out.add(p);
        }
        out.sort(Comparator.comparingDouble(p -> p.feet().get().dist(s.feet())));
        return out;
    }

    @Override
    public Shot plan(Scene s, Random rnd) {
        List<Scene.Subject> list = candidates(s);
        if (list.size() > 1) list.sort(Comparator.comparing(p -> p.id().equals(last)));
        Tries tries = new Tries(s);
        for (Scene.Subject p : list) {
            double base = rnd.nextDouble() * 360;
            for (int i = 0; i < 6 && !tries.exhausted(); i++) {
                Shot shot = style(s, p, base + i * 60, rnd, i + rnd.nextInt(3));
                if (tries.accept(shot, null) && ShotCheck.sees(s, shot, p.head())) {
                    last = p.id();
                    return shot;
                }
            }
        }
        return null;
    }

    static Shot style(Scene s, Scene.Subject p, double angle, Random r, int kind) {
        double look = p.height() * 0.8;
        return switch (Math.floorMod(kind, 3)) {
            case 0 -> OrbitShot.around(V3.ZERO, Tries.range(r, 3.2, 4.5), Tries.range(r, 1.2, 1.9), angle,
                    angle + (r.nextBoolean() ? 1 : -1) * Tries.range(r, 80, 120), look, s.time(12), 50).following(p.feet(), p.name());
            case 1 -> {
                V3 dir = V3.heading(angle), side = dir.side();
                yield PathShot.along(Spline.of(
                                dir.mul(-3.5).add(side.mul(-4)).add(0, 1.6, 0),
                                dir.mul(-2.6).add(0, 1.4, 0),
                                dir.mul(-3.5).add(side.mul(4)).add(0, 1.7, 0)), s.time(9))
                        .lookAt(new V3(0, look, 0)).fov(55, 52).relativeTo(p.feet(), p.name()).build();
            }
            default -> OrbitShot.around(V3.ZERO, 5.5, 1.8, angle, angle + 40, look, s.time(11), 48)
                    .drift(3.2, 1.4).following(p.feet(), p.name());
        };
    }
}
