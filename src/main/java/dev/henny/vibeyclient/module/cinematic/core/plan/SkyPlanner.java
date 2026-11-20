package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.Clearance;
import dev.henny.vibeyclient.module.cinematic.core.V3;
import dev.henny.vibeyclient.module.cinematic.core.shot.PanShot;
import dev.henny.vibeyclient.module.cinematic.core.shot.Shot;

import java.util.Random;

public final class SkyPlanner implements Planner {
    static final int OPEN = 48;
    private boolean tilt;

    @Override
    public boolean possible(Scene s) {
        return Clearance.openSky(s.world(), s.eye(), OPEN);
    }

    @Override
    public Shot plan(Scene s, Random rnd) {
        Tries tries = new Tries(s, Tries.CLOSE, 0);
        for (int i = 0; i < 6 && !tries.exhausted(); i++) {
            V3 from = s.eye().add(V3.heading(rnd.nextDouble() * 360).mul(i == 0 ? 0 : 2)).add(0, Tries.range(rnd, 1.5, 3), 0);
            if (!Clearance.openSky(s.world(), from, OPEN)) continue;
            double yaw = s.sky().sunLow() ? s.sky().sunYaw() : rnd.nextDouble() * 360;
            double turn = Tries.range(rnd, 20, 35) * (rnd.nextBoolean() ? 1 : -1);
            tilt = !tilt;
            Shot shot = tilt
                    ? new PanShot(from, from.add(0, 1, 0), (float) yaw, (float) (yaw + turn), 2, -62, s.time(18), 80)
                    : new PanShot(from, from.add(0, 0.6, 0), (float) yaw, (float) (yaw + turn), -58, -72, s.time(20), 88);
            if (tries.accept(shot, null)) return shot;
        }
        return null;
    }
}
