package dev.henny.r3.anchor;

import dev.henny.r3.math.Face;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class BlockHit {
    private final Vector3d point = new Vector3d();
    private Face face = Face.UP;
    private int blockX;
    private int blockY;
    private int blockZ;
    private double distance;

    public BlockHit set(double x, double y, double z, Face face, int blockX, int blockY, int blockZ, double distance) {
        point.set(x, y, z);
        this.face = face;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.distance = distance;
        return this;
    }

    public Vector3dc point() {
        return point;
    }

    public Face face() {
        return face;
    }

    public int blockX() {
        return blockX;
    }

    public int blockY() {
        return blockY;
    }

    public int blockZ() {
        return blockZ;
    }

    public double distance() {
        return distance;
    }
}
