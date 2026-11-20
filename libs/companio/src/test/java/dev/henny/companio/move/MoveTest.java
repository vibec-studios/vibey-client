package dev.henny.companio.move;

import dev.henny.companio.support.GridView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveTest {
    private static Body body(double x, double y, double z) {
        Body b = new Body();
        b.place(x, y, z);
        return b;
    }

    private static void steps(Body b, GridView v, MoveIntent in, int n) {
        for (int i = 0; i < n; i++) {
            b.beginTick();
            Physics.step(b, v, in, 0.6f);
        }
    }

    @Test
    void fallsAndLands() {
        GridView v = new GridView().floor(63);
        Body b = body(0.5, 70, 0.5);
        steps(b, v, new MoveIntent(), 60);
        assertEquals(64.0, b.y, 1e-6);
        assertTrue(b.onGround);
    }

    @Test
    void stepsUpSlabsButNotWalls() {
        GridView v = new GridView().floor(63).wall(7, -3, 3, 64, 66);
        for (int x = 2; x <= 6; x++) v.top(x, 64, 0, 0.5f);
        Body b = body(0.5, 64, 0.5);
        MoveIntent in = new MoveIntent();
        in.vx = 0.15;
        steps(b, v, in, 60);
        assertEquals(64.5, b.y, 1e-6, "walked onto the slab");
        assertTrue(b.x < 7 - b.width / 2 + 1e-3 && b.x > 6, "stopped at the wall, x " + b.x);
        assertTrue(b.blocked);
    }

    @Test
    void jumpsOneBlock() {
        GridView v = new GridView().floor(63).solid(2, 64, 0);
        Body b = body(1.5, 64, 0.5);
        MoveIntent in = new MoveIntent();
        in.vx = 0.1;
        in.jump = true;
        in.jumpHeight = 1.1f;
        steps(b, v, in, 30);
        assertTrue(b.y >= 65 - 1e-6, "on top of the block, y " + b.y);
    }

    @Test
    void floatsInWater() {
        GridView v = new GridView().floor(59);
        for (int y = 60; y <= 63; y++) v.fluid(0, y, 0, Fluid.WATER);
        Body b = body(0.5, 62, 0.5);
        b.height = 0.6f;
        MoveIntent in = new MoveIntent();
        in.swim = true;
        steps(b, v, in, 120);
        assertTrue(b.inWater);
        assertEquals(63.9 - 0.27, b.y, 0.15);
    }

    @Test
    void pathGoesAroundWallsAndAvoidsLava() {
        GridView v = new GridView().floor(63).wall(3, -4, 4, 64, 65);
        for (int z = -2; z <= 2; z++) v.fluid(1, 63, z, Fluid.LAVA);
        Terrain t = new Terrain(v, 0.6f, true);
        List<int[]> path = PathFinder.find(t, 0, 64, 0, 6, 64, 0, 0.5, true);
        assertFalse(path.isEmpty());
        int[] last = path.get(path.size() - 1);
        assertEquals(6, last[0]);
        for (int[] n : path) {
            assertFalse(n[0] == 3 && Math.abs(n[2]) <= 4, "walked through the wall");
            assertFalse(Math.abs(n[0] - 1) <= 1 && Math.abs(n[2]) <= 3, "next to lava at " + n[0] + "," + n[2]);
        }
    }

    @Test
    void neverDropsMoreThanThreeBlocks() {
        GridView v = new GridView().floor(53);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) v.solid(x, 63, z);
        Terrain t = new Terrain(v, 0.6f, true);
        List<int[]> path = PathFinder.find(t, 0, 64, 0, 8, 54, 0, 0.5, true);
        for (int[] n : path) assertTrue(n[1] >= 61 || Math.abs(n[0]) <= 1, "dropped down a cliff");
    }

    @Test
    void anglesWrap() {
        assertEquals(-170f, Angles.wrap(190f), 1e-4);
        assertEquals(10f, Angles.approach(0f, 90f, 10f), 1e-4);
        assertEquals(175f, Angles.approach(170f, -170f, 5f), 1e-4);
        assertEquals(0f, Angles.yawTo(0, 1), 1e-4);
        assertEquals(90f, Angles.yawTo(-1, 0), 1e-4);
    }
}
