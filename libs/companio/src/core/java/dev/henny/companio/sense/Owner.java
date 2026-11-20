package dev.henny.companio.sense;

public final class Owner {
    public boolean present;
    public double x, y, z, eyeY;
    public float yaw, bodyYaw, pitch;
    public double vx, vy, vz;
    public boolean sneaking, sprinting, swimming, elytra, flying, sleeping, eating, onGround = true, inWater, riding;
    public boolean hurt, jumped, looking;
    public float health = 1f;
    public double idle, speed;
    public String held = "minecraft:air", offhand = "minecraft:air";

    public void copyFrom(Owner o) {
        present = o.present;
        x = o.x; y = o.y; z = o.z; eyeY = o.eyeY;
        yaw = o.yaw; bodyYaw = o.bodyYaw; pitch = o.pitch;
        vx = o.vx; vy = o.vy; vz = o.vz;
        sneaking = o.sneaking; sprinting = o.sprinting; swimming = o.swimming; elytra = o.elytra; flying = o.flying;
        sleeping = o.sleeping; eating = o.eating; onGround = o.onGround; inWater = o.inWater; riding = o.riding;
        hurt = o.hurt; jumped = o.jumped; looking = o.looking;
        health = o.health; idle = o.idle; speed = o.speed; held = o.held; offhand = o.offhand;
    }
}
