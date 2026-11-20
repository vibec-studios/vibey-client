package dev.henny.companio.sense;

public final class Senses {
    public static final double FAR = 99;

    public final Owner owner = new Owner();
    public boolean raining, thundering, cold, hot, sky;
    public long dayTime = 6000;
    public String dimension = "minecraft:overworld", biome = "minecraft:plains";
    public int light = 15;
    public double nearWater = FAR, nearLava = FAR, nearFire = FAR, nearMob = FAR, nearPlayer = FAR, nearCompanion = FAR;
    public double dangerX, dangerZ;
    public boolean danger;

    public long timeOfDay() {
        return Math.floorMod(dayTime, 24000L);
    }

    public boolean night() {
        long t = timeOfDay();
        return t >= 13000 && t < 23000;
    }

    public double hour() {
        return ((timeOfDay() / 1000.0) + 6.0) % 24.0;
    }

    public void copyFrom(Senses s) {
        owner.copyFrom(s.owner);
        raining = s.raining; thundering = s.thundering; cold = s.cold; hot = s.hot; sky = s.sky;
        dayTime = s.dayTime; dimension = s.dimension; biome = s.biome; light = s.light;
        nearWater = s.nearWater; nearLava = s.nearLava; nearFire = s.nearFire; nearMob = s.nearMob;
        nearPlayer = s.nearPlayer; nearCompanion = s.nearCompanion;
        dangerX = s.dangerX; dangerZ = s.dangerZ; danger = s.danger;
    }
}
