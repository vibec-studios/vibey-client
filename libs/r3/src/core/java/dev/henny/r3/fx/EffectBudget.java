package dev.henny.r3.fx;

public final class EffectBudget {
    public static final EffectBudget UNLIMITED = new EffectBudget(Integer.MAX_VALUE, 1e9, 1e9, 1e9);

    private final int maxNewParticlesPerFrame;
    private final double fullDetailDistance;
    private final double fadeOutDistance;
    private final double cullDistance;
    private int grantedThisFrame;

    public EffectBudget(int maxNewParticlesPerFrame, double fullDetailDistance, double fadeOutDistance, double cullDistance) {
        this.maxNewParticlesPerFrame = maxNewParticlesPerFrame;
        this.fullDetailDistance = fullDetailDistance;
        this.fadeOutDistance = Math.max(fullDetailDistance, fadeOutDistance);
        this.cullDistance = Math.max(this.fadeOutDistance, cullDistance);
    }

    public static EffectBudget standard() {
        return new EffectBudget(2048, 16, 48, 96);
    }

    public void newFrame() {
        grantedThisFrame = 0;
    }

    public int take(int wanted) {
        int granted = Math.max(0, Math.min(wanted, maxNewParticlesPerFrame - grantedThisFrame));
        grantedThisFrame += granted;
        return granted;
    }

    public float lodAt(double distance) {
        if (distance >= cullDistance) return 0f;
        if (distance <= fullDetailDistance) return 1f;
        if (distance >= fadeOutDistance) return 0.1f;
        double k = (distance - fullDetailDistance) / (fadeOutDistance - fullDetailDistance);
        return (float) (1.0 - 0.9 * k);
    }

    public boolean culled(double distance) {
        return distance >= cullDistance;
    }
}
