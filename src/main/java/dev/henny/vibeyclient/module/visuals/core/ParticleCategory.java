package dev.henny.vibeyclient.module.visuals.core;

public enum ParticleCategory {
    IMPORTANT, AMBIENT, WEATHER, BLOCK, EFFECT, OTHER;

    public boolean ambient() {
        return this == AMBIENT || this == WEATHER;
    }
}
