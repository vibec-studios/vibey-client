package dev.henny.cosmetico.skeleton;

import dev.henny.cosmetico.model.Animation;

import java.util.Arrays;

public final class Mix {
    public static final int MAX = 8;
    public final Animation[] anims = new Animation[MAX];
    public final float[] times = new float[MAX];
    public final float[] weights = new float[MAX];
    public int count;
    public Animation visibility;
    public float visibilityTime;
    public final float[] rootOffset = new float[3], rootRotation = new float[3];
    public boolean hasRootOffset;

    public Mix clear() {
        count = 0;
        visibility = null;
        if (hasRootOffset) {
            Arrays.fill(rootOffset, 0f);
            Arrays.fill(rootRotation, 0f);
            hasRootOffset = false;
        }
        return this;
    }

    public Mix add(Animation a, float t, float w) {
        if (a == null || w <= 0.001f || count == MAX) return this;
        anims[count] = a;
        times[count] = t;
        weights[count] = w;
        count++;
        return this;
    }

    public Mix single(Animation a, float t) {
        clear().add(a, t, 1f);
        visibility = a;
        visibilityTime = t;
        return this;
    }
}
