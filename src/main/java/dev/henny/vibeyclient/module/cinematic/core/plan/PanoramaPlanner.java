package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.V3;
import dev.henny.vibeyclient.module.cinematic.core.shot.PanShot;
import dev.henny.vibeyclient.module.cinematic.core.shot.Shot;

import java.util.Random;

public final class PanoramaPlanner implements Planner {
    @Override
    public Shot plan(Scene s, Random rnd) {
        Tries tries = new Tries(s);
        double top = Math.min(22, s.radius() * 0.5);
        for (double h : new double[] {top, top * 0.7, top * 0.45, 4}) {
            for (int i = 0; i < 4 && !tries.exhausted(); i++) {
                V3 lateral = V3.heading(rnd.nextDouble() * 360).mul(i == 0 ? 0 : Tries.range(rnd, 2, 6));
                V3 from = s.eye().add(lateral).add(0, h, 0);
                double center = s.sky().sunLow() ? s.sky().sunYaw() + Tries.range(rnd, -15, 15) : rnd.nextDouble() * 360;
                double sweep = Tries.range(rnd, 35, 55) * (rnd.nextBoolean() ? 1 : -1);
                V3 drift = V3.heading(center + 90).mul(sweep > 0 ? 2 : -2).add(0, 1.5, 0);
                float pitch = (float) (h > 12 ? 12 : 7);
                Shot shot = new PanShot(from, from.add(drift), (float) (center - sweep / 2), (float) (center + sweep / 2),
                        pitch, pitch - 4, s.time(22), 75);
                if (tries.accept(shot, null)) return shot;
            }
        }
        return null;
    }
}
