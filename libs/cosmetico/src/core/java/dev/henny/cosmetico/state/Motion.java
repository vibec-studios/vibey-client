package dev.henny.cosmetico.state;

import java.util.Arrays;

public final class Motion {
    public float walk, fly, sneak, sprint, glide, idleLong, happy;
    public float walkTarget, flyTarget, sneakTarget, sprintTarget, glideTarget, idleLongTarget, happyTarget;
    public int stillTicks, fallTicks;
    public boolean moveInit;
    public final double[] stateStart = new double[8];

    public Motion() {
        Arrays.fill(stateStart, Double.NaN);
    }

    public void still() {
        walk = fly = sneak = 0;
        sprint = glide = idleLong = 0;
    }
}
