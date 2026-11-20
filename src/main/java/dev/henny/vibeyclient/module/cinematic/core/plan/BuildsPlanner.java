package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.Spline;
import dev.henny.vibeyclient.module.cinematic.core.V3;
import dev.henny.vibeyclient.module.cinematic.core.shot.OrbitShot;
import dev.henny.vibeyclient.module.cinematic.core.shot.PathShot;
import dev.henny.vibeyclient.module.cinematic.core.shot.Shot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public final class BuildsPlanner implements Planner {
    private List<Poi> pois = List.of();
    private V3 scannedAt;
    private final Deque<V3> recent = new ArrayDeque<>();

    public List<Poi> pois(Scene s) {
        if (scannedAt == null || scannedAt.dist(s.feet()) > 6) {
            pois = PoiScanner.scan(s.world(), s.feet(), Math.min(32, s.radius() - 4));
            scannedAt = s.feet();
        }
        return pois;
    }

    @Override
    public Shot plan(Scene s, Random rnd) {
        Tries tries = new Tries(s);
        for (Poi p : order(pois(s), rnd)) {
            if (tries.exhausted()) break;
            if (!ShotCheck.visible(s, p.focus())) continue;
            double base = rnd.nextDouble() * 360;
            for (int i = 0; i < 4 && !tries.exhausted(); i++) {
                Shot shot = style(s, p.focus(), base + i * 90, rnd, i + rnd.nextInt(3));
                if (tries.accept(shot, p.focus())) {
                    remember(p.focus());
                    return shot;
                }
            }
        }
        return null;
    }

    private List<Poi> order(List<Poi> all, Random rnd) {
        List<Poi> fresh = new ArrayList<>(), seen = new ArrayList<>();
        for (Poi p : all) (wasRecent(p.focus()) ? seen : fresh).add(p);
        if (fresh.size() > 1) {
            int pick = rnd.nextInt(Math.min(3, fresh.size()));
            fresh.add(0, fresh.remove(pick));
        }
        fresh.addAll(seen);
        return fresh;
    }

    private boolean wasRecent(V3 p) {
        for (V3 r : recent) if (r.dist(p) < 4) return true;
        return false;
    }

    private void remember(V3 p) {
        recent.addFirst(p);
        while (recent.size() > 4) recent.removeLast();
    }

    static Shot style(Scene s, V3 poi, double yaw, Random r, int kind) {
        V3 dir = V3.heading(yaw), side = dir.side();
        double d = Tries.range(r, 6, 10), h = Tries.range(r, 2.5, 5), l = Tries.range(r, 5, 8);
        return switch (Math.floorMod(kind, 3)) {
            case 0 -> PathShot.along(Spline.of(
                            poi.sub(dir.mul(d)).add(side.mul(-l)).add(0, h, 0),
                            poi.sub(dir.mul(d * 0.8)).add(0, h * 0.85, 0),
                            poi.sub(dir.mul(d)).add(side.mul(l)).add(0, h, 0)), s.time(14))
                    .lookAlong(Spline.of(poi.add(side.mul(-1.5)), poi.add(side.mul(1.5)))).fov(62, 58).build();
            case 1 -> PathShot.along(Spline.of(
                            poi.sub(dir.mul(d * 0.6)).add(0, 1.2, 0),
                            poi.sub(dir.mul(d + 3)).add(0, h + 5, 0)), s.time(12))
                    .lookAlong(Spline.of(poi, poi.add(0, 1, 0))).fov(55, 68).build();
            default -> OrbitShot.around(poi, d, h, -yaw, -yaw + (r.nextBoolean() ? 70 : -70), 0, s.time(16), 60);
        };
    }
}
