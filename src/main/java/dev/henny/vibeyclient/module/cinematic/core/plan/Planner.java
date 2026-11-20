package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.shot.Shot;

import java.util.Random;

public interface Planner {
    Shot plan(Scene s, Random rnd);

    default boolean possible(Scene s) {
        return true;
    }
}
