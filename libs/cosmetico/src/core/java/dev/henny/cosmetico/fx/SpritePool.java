package dev.henny.cosmetico.fx;

import dev.henny.cosmetico.model.Bundle;

import java.util.Random;

public final class SpritePool {
    public static final int MAX = 256;
    public final double[] x = new double[MAX], y = new double[MAX], z = new double[MAX];
    public final double[] px = new double[MAX], py = new double[MAX], pz = new double[MAX];
    public final float[] vx = new float[MAX], vy = new float[MAX], vz = new float[MAX];
    public final float[] age = new float[MAX], life = new float[MAX], size = new float[MAX], gravity = new float[MAX];
    public final Bundle[] bundle = new Bundle[MAX];
    public final int[] sprite = new int[MAX];
    public final boolean[] emissive = new boolean[MAX];
    public int count;
    private final Random random;

    public SpritePool(Random random) {
        this.random = random;
    }

    public void spawn(Bundle b, SpriteSpawn s) {
        for (int k = 0; k < s.count(); k++) spawnOne(b, s);
    }

    private void spawnOne(Bundle b, SpriteSpawn s) {
        if (count >= MAX) return;
        int i = count++;
        x[i] = px[i] = s.x() + (random.nextFloat() * 2 - 1) * s.sx();
        y[i] = py[i] = s.y() + (random.nextFloat() * 2 - 1) * s.sy();
        z[i] = pz[i] = s.z() + (random.nextFloat() * 2 - 1) * s.sz();
        vx[i] = s.vx() + (random.nextFloat() - 0.5f) * 0.01f;
        vy[i] = s.vy() + (random.nextFloat() - 0.5f) * 0.01f;
        vz[i] = s.vz() + (random.nextFloat() - 0.5f) * 0.01f;
        age[i] = 0;
        life[i] = Math.max(1f, s.lifetime() * 20f * (0.85f + random.nextFloat() * 0.3f));
        size[i] = s.size();
        gravity[i] = s.gravity();
        bundle[i] = b;
        sprite[i] = s.sprite();
        emissive[i] = s.emissive();
    }

    public void tick() {
        for (int i = 0; i < count; i++) {
            px[i] = x[i];
            py[i] = y[i];
            pz[i] = z[i];
            x[i] += vx[i];
            y[i] += vy[i];
            z[i] += vz[i];
            vy[i] -= gravity[i];
            vx[i] *= 0.98f;
            vy[i] *= 0.98f;
            vz[i] *= 0.98f;
            if (++age[i] >= life[i]) {
                int last = --count;
                SpriteSlots.move(this, last, i);
                bundle[last] = null;
                i--;
            }
        }
    }
}
