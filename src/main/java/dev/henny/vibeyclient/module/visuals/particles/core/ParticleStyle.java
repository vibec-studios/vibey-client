package dev.henny.vibeyclient.module.visuals.particles.core;

public record ParticleStyle(int amount, int size, int tint, int strength, boolean hidden) {
    public static final int MAX_AMOUNT = 400;
    public static final int MIN_SIZE = 25;
    public static final int MAX_SIZE = 300;
    public static final int MAX_STRENGTH = 100;
    public static final int WHITE = 0xFFFFFF;
    public static final ParticleStyle VANILLA = new ParticleStyle(100, 100, WHITE, 0, false);

    public ParticleStyle {
        amount = clamp(amount, 0, MAX_AMOUNT);
        size = clamp(size, MIN_SIZE, MAX_SIZE);
        tint = tint & 0xFFFFFF;
        strength = clamp(strength, 0, MAX_STRENGTH);
    }

    public boolean vanilla() {
        return amount == 100 && size == 100 && strength == 0 && !hidden;
    }

    public boolean tinted() {
        return strength > 0;
    }

    public boolean reduces() {
        return hidden || amount < 100 || size < 100;
    }

    public ParticleStyle fair() {
        if (!reduces()) return this;
        return new ParticleStyle(Math.max(100, amount), Math.max(100, size), tint, strength, false);
    }

    public ParticleStyle withAmount(int v) {
        return new ParticleStyle(v, size, tint, strength, hidden);
    }

    public ParticleStyle withSize(int v) {
        return new ParticleStyle(amount, v, tint, strength, hidden);
    }

    public ParticleStyle withTint(int rgb) {
        return new ParticleStyle(amount, size, rgb, strength, hidden);
    }

    public ParticleStyle withStrength(int v) {
        return new ParticleStyle(amount, size, tint, v, hidden);
    }

    public ParticleStyle withHidden(boolean v) {
        return new ParticleStyle(amount, size, tint, strength, v);
    }

    public float sizeFactor() {
        return size / 100f;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
