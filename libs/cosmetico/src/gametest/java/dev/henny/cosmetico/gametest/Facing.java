package dev.henny.cosmetico.gametest;

import net.minecraft.client.player.RemotePlayer;

final class Facing {
    private Facing() {}

    static void set(RemotePlayer p, float yaw) {
        p.setYRot(yaw);
        p.yRotO = yaw;
        p.yBodyRot = yaw;
        p.yBodyRotO = yaw;
        p.yHeadRot = yaw;
        p.yHeadRotO = yaw;
        p.setXRot(0);
        p.xRotO = 0;
    }
}
