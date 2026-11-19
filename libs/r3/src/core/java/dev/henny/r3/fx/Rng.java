package dev.henny.r3.fx;

public final class Rng {
    private long state;

    public Rng(long seed) {
        seed(seed);
    }

    public void seed(long seed) {
        state = seed == 0 ? 0x9E3779B97F4A7C15L : seed;
    }

    public long nextLong() {
        state ^= state >>> 12;
        state ^= state << 25;
        state ^= state >>> 27;
        return state * 0x2545F4914F6CDD1DL;
    }

    public float nextFloat() {
        return (nextLong() >>> 40) / (float) (1 << 24);
    }

    public float signed() {
        return nextFloat() * 2f - 1f;
    }

    public float range(float min, float max) {
        return min + (max - min) * nextFloat();
    }
}
