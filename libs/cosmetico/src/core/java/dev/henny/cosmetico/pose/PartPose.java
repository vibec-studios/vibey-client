package dev.henny.cosmetico.pose;

public final class PartPose {
    public float x, y, z, xRot, yRot, zRot, xScale = 1f, yScale = 1f, zScale = 1f;

    public PartPose set(float x, float y, float z, float xRot, float yRot, float zRot, float xScale, float yScale, float zScale) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.xRot = xRot;
        this.yRot = yRot;
        this.zRot = zRot;
        this.xScale = xScale;
        this.yScale = yScale;
        this.zScale = zScale;
        return this;
    }
}
