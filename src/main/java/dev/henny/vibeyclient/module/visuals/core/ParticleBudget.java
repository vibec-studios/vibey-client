package dev.henny.vibeyclient.module.visuals.core;

public final class ParticleBudget {
    private double carry;

    public int next(double multiplier) {
        if (!(multiplier > 0)) {
            carry = 0;
            return 0;
        }
        double want = multiplier + carry;
        int n = (int) Math.floor(want);
        carry = want - n;
        return n;
    }

    public double carry() {
        return carry;
    }
}
