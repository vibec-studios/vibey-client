package dev.henny.companio.move;

public final class MoveIntent {
    public double vx, vz, vy;
    public boolean jump, swim, fly, frozen;
    public float jumpHeight = 1.1f;

    public void clear() {
        vx = vz = vy = 0;
        jump = swim = fly = frozen = false;
    }
}
