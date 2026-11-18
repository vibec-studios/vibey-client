package dev.henny.vibeyclient.module.hud;

import dev.henny.vibeyclient.client.ClientCompat;
import net.minecraft.client.Minecraft;

/**
 * Clicks per second of the attack and use keys, sampled every frame (no event hooks needed). Only counts while no
 * screen is open. Ring buffers of press times, no allocation.
 */
final class Clicks {
    private Clicks() {}

    private static final long[] LEFT = new long[64];
    private static final long[] RIGHT = new long[64];
    private static int leftN, rightN;
    private static boolean leftDown, rightDown;

    static void sample() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        boolean inGame = ClientCompat.screen() == null && mc.player != null;
        boolean l = inGame && mc.options.keyAttack.isDown();
        boolean r = inGame && mc.options.keyUse.isDown();
        long now = System.currentTimeMillis();
        if (l && !leftDown) LEFT[leftN++ & 63] = now;
        if (r && !rightDown) RIGHT[rightN++ & 63] = now;
        leftDown = l;
        rightDown = r;
    }

    static int left() {
        return count(LEFT, leftN);
    }

    static int right() {
        return count(RIGHT, rightN);
    }

    static boolean leftDown() {
        return leftDown;
    }

    static boolean rightDown() {
        return rightDown;
    }

    private static int count(long[] ring, int n) {
        long cut = System.currentTimeMillis() - 1000;
        int c = 0;
        for (int i = 0; i < Math.min(n, ring.length); i++) if (ring[i] > cut) c++;
        return c;
    }
}
