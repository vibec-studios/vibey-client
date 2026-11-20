package dev.henny.companio.move;

final class Goal {
    double x, y, z, stopWithin, speed;
    boolean active, direct;

    Goal set(double x, double y, double z, double stopWithin, double speed) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.stopWithin = stopWithin;
        this.speed = speed;
        this.active = true;
        this.direct = false;
        return this;
    }

    void none() {
        active = false;
    }
}
