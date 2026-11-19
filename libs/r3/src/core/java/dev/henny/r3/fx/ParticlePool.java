package dev.henny.r3.fx;

public final class ParticlePool {
    final double[] x;
    final double[] y;
    final double[] z;
    final float[] vx;
    final float[] vy;
    final float[] vz;
    final float[] age;
    final float[] life;
    final float[] size;
    final float[] spin;
    private final int capacity;
    private int count;

    public ParticlePool(int capacity) {
        this.capacity = capacity;
        x = new double[capacity];
        y = new double[capacity];
        z = new double[capacity];
        vx = new float[capacity];
        vy = new float[capacity];
        vz = new float[capacity];
        age = new float[capacity];
        life = new float[capacity];
        size = new float[capacity];
        spin = new float[capacity];
    }

    public int capacity() {
        return capacity;
    }

    public int count() {
        return count;
    }

    public int spawn(double px, double py, double pz, float velX, float velY, float velZ, float lifetime, float baseSize, float spinSpeed) {
        if (count >= capacity) return -1;
        int i = count++;
        x[i] = px;
        y[i] = py;
        z[i] = pz;
        vx[i] = velX;
        vy[i] = velY;
        vz[i] = velZ;
        age[i] = 0f;
        life[i] = Math.max(1e-3f, lifetime);
        size[i] = baseSize;
        spin[i] = spinSpeed;
        return i;
    }

    public void simulate(float dt, float gravity, float drag) {
        float damp = Math.max(0f, 1f - drag * dt);
        for (int i = 0; i < count; i++) {
            vy[i] -= gravity * dt;
            vx[i] *= damp;
            vy[i] *= damp;
            vz[i] *= damp;
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
            z[i] += vz[i] * dt;
            age[i] += dt;
            if (age[i] >= life[i]) {
                moveLastTo(i);
                i--;
            }
        }
    }

    private void moveLastTo(int i) {
        int last = --count;
        if (i == last) return;
        x[i] = x[last];
        y[i] = y[last];
        z[i] = z[last];
        vx[i] = vx[last];
        vy[i] = vy[last];
        vz[i] = vz[last];
        age[i] = age[last];
        life[i] = life[last];
        size[i] = size[last];
        spin[i] = spin[last];
    }

    public float lifeFraction(int i) {
        return Math.min(1f, age[i] / life[i]);
    }

    public double x(int i) {
        return x[i];
    }

    public double y(int i) {
        return y[i];
    }

    public double z(int i) {
        return z[i];
    }

    public void clear() {
        count = 0;
    }
}
