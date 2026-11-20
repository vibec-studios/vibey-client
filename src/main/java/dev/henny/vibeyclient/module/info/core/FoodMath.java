package dev.henny.vibeyclient.module.info.core;

public final class FoodMath {
    public static final int MAX_FOOD = 20;

    private FoodMath() {}

    public static FoodGain eat(int food, float saturation, int nutrition, float saturationGain) {
        int before = clamp(food, 0, MAX_FOOD);
        int after = clamp(before + nutrition, 0, MAX_FOOD);
        float satBefore = Math.max(0f, Math.min(saturation, before));
        float satAfter = Math.max(0f, Math.min(satBefore + saturationGain, after));
        return new FoodGain(after, satAfter, after - before, satAfter - satBefore, before + nutrition > MAX_FOOD);
    }

    public static float saturationOf(int nutrition, float modifier) {
        return nutrition * modifier * 2f;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
