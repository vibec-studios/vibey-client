package dev.henny.cosmetico.state;

public final class PetMotion {
    public double x, y, z, prevX, prevY, prevZ;
    public float yaw, prevYaw, move;
    public boolean init;

    public void keepPrevious() {
        prevX = x;
        prevY = y;
        prevZ = z;
        prevYaw = yaw;
    }

    public void place(double px, double py, double pz, float yawDeg) {
        x = prevX = px;
        y = prevY = py;
        z = prevZ = pz;
        yaw = prevYaw = yawDeg;
    }
}
