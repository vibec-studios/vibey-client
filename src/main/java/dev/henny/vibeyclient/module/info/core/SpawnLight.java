package dev.henny.vibeyclient.module.info.core;

public enum SpawnLight {
    SAFE(0xFF3FE0C5), NIGHT(0xFFFFE14D), ALWAYS(0xFFFF5470);

    private final int color;

    SpawnLight(int color) {
        this.color = color;
    }

    public int color() {
        return color;
    }

    public static SpawnLight classify(int blockLight, int skyLight, int blockLimit, int darknessMax) {
        if (blockLight > blockLimit || blockLight > darknessMax) return SAFE;
        return skyLight <= darknessMax ? ALWAYS : NIGHT;
    }
}
