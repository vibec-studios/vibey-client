package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.shot.PanShot;
import dev.henny.vibeyclient.module.cinematic.core.shot.Shot;

import java.util.Random;

public final class StillPlanner implements Planner {
    @Override
    public Shot plan(Scene s, Random rnd) {
        float yaw = (float) s.facing();
        float turn = (rnd.nextBoolean() ? 1 : -1) * 25f;
        return new PanShot(s.eye(), s.eye(), yaw - turn / 2, yaw + turn / 2, 4, 0, s.time(14), 68);
    }
}
