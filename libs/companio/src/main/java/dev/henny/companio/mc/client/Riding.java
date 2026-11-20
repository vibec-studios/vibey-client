package dev.henny.companio.mc.client;

import dev.henny.companio.def.MoveMode;
import dev.henny.companio.world.Companion;
import net.minecraft.world.entity.player.Player;

final class Riding {
    private final double[] p = new double[4];

    void snap(Companion c, Player player) {
        if (player == null || !position(c, player, 1f, p)) return;
        c.holdAt(p[0], p[1], p[2]);
        c.body.yaw = (float) p[3];
    }

    boolean position(Companion c, Player player, float pt, double[] out) {
        if (player == null) return false;
        float[] seat = c.riding() == MoveMode.HEAD ? c.def.movement().head() : c.def.movement().shoulder();
        double x = player.xo + (player.getX() - player.xo) * pt;
        double y = player.yo + (player.getY() - player.yo) * pt;
        double z = player.zo + (player.getZ() - player.zo) * pt;
        float yaw = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * pt;
        double r = Math.toRadians(yaw);
        double rx = -Math.cos(r), rz = -Math.sin(r), bx = Math.sin(r), bz = -Math.cos(r);
        double crouch = player.isShiftKeyDown() ? -0.3 : 0;
        out[0] = x + rx * seat[0] + bx * seat[2];
        out[1] = y + seat[1] + crouch;
        out[2] = z + rz * seat[0] + bz * seat[2];
        out[3] = yaw;
        return true;
    }
}
