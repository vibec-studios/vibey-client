package org.lwjgl.openal;

public final class AL10 {
    public static float gain = -1f;
    public static float other = -1f;

    public static void alListenerf(int param, float value) {
        if (param == 0x100A) gain = value;
        else other = value;
    }
}
