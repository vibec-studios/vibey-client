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
    public boolean[] hidden;
    public static final int MAX_EXTRA = 4;
    public final int[] extraBones = new int[MAX_EXTRA];
    public final float[] extraRotations = new float[MAX_EXTRA * 3];
    public int extraCount;

    public Mix clear() {
        count = 0;
        visibility = null;
        hidden = null;
        extraCount = 0;
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

    public Mix rotate(int bone, float x, float y, float z) {
        if (bone < 0 || extraCount == MAX_EXTRA) return this;
        extraBones[extraCount] = bone;
        extraRotations[extraCount * 3] = x;
        extraRotations[extraCount * 3 + 1] = y;
        extraRotations[extraCount * 3 + 2] = z;
        extraCount++;
        return this;
    }

    public Mix single(Animation a, float t) {
        clear().add(a, t, 1f);
        visibility = a;
        visibilityTime = t;
        return this;
    }
}
