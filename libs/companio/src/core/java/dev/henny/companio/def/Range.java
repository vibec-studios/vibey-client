package dev.henny.companio.def;

import java.util.Random;

public record Range(float min, float max) {
    public static Range of(float v) {
        return new Range(v, v);
    }

    public float pick(Random r) {
        return max <= min ? min : min + r.nextFloat() * (max - min);
    }
}
