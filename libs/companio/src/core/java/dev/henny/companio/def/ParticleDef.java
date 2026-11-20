package dev.henny.companio.def;

public record ParticleDef(String id, String particle, int count, String locator, float[] spread, float[] velocity) {
    public static final float[] ZERO = {0f, 0f, 0f};
}
