package dev.henny.companio.move;

public final class Home {
    public boolean set;
    public double x, y, z;

    public void set(double x, double y, double z) {
        this.set = true;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void clear() {
        set = false;
    }
}
