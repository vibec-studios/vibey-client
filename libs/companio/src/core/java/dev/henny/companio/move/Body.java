package dev.henny.companio.move;

public final class Body {
    public double x, y, z, px, py, pz;
    public double vx, vy, vz;
    public float yaw, pyaw;
    public float width = 0.6f, height = 0.6f;
    public boolean onGround, inWater, inLava, blocked;
    public double waterTop = Double.NaN;
    public double airTime, fallFrom;
    public Surface ground = Surface.NORMAL;

    public void place(double x, double y, double z) {
        this.x = px = x;
        this.y = py = y;
        this.z = pz = z;
        vx = vy = vz = 0;
        fallFrom = y;
    }

    public void beginTick() {
        px = x;
        py = y;
        pz = z;
        pyaw = yaw;
    }

    public double speed() {
        return Math.sqrt(vx * vx + vz * vz) * 20;
    }

    public double distance(double ox, double oy, double oz) {
        double dx = ox - x, dy = oy - y, dz = oz - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double flatDistance(double ox, double oz) {
        double dx = ox - x, dz = oz - z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
