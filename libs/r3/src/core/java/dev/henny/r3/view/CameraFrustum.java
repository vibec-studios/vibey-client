package dev.henny.r3.view;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class CameraFrustum implements ViewFrustum {
    private final Vector3d position = new Vector3d();
    private final Vector3d look = new Vector3d(0, 0, 1);
    private final Vector3d right = new Vector3d(-1, 0, 0);
    private final Vector3d up = new Vector3d(0, 1, 0);
    private double tanX = 1;
    private double tanY = 1;
    private double normX = Math.sqrt(2);
    private double normY = Math.sqrt(2);

    public CameraFrustum set(CameraState camera, double verticalFovDegrees, double aspect, double marginDegrees) {
        position.set(camera.position());
        look.set(camera.look());
        right.set(camera.right());
        up.set(camera.up());
        double half = Math.min(89.0, verticalFovDegrees / 2.0 + marginDegrees);
        tanY = Math.tan(Math.toRadians(half));
        tanX = tanY * Math.max(0.1, aspect) + Math.tan(Math.toRadians(marginDegrees));
        normX = Math.sqrt(1 + tanX * tanX);
        normY = Math.sqrt(1 + tanY * tanY);
        return this;
    }

    public Vector3dc position() {
        return position;
    }

    @Override
    public boolean isVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double cx = (minX + maxX) / 2 - position.x;
        double cy = (minY + maxY) / 2 - position.y;
        double cz = (minZ + maxZ) / 2 - position.z;
        double hx = (maxX - minX) / 2, hy = (maxY - minY) / 2, hz = (maxZ - minZ) / 2;
        double r = Math.sqrt(hx * hx + hy * hy + hz * hz);
        if (cx * cx + cy * cy + cz * cz <= r * r) return true;
        double z = cx * look.x + cy * look.y + cz * look.z;
        if (z < -r) return false;
        double x = cx * right.x + cy * right.y + cz * right.z;
        double y = cx * up.x + cy * up.y + cz * up.z;
        if ((Math.abs(x) - z * tanX) / normX > r) return false;
        return (Math.abs(y) - z * tanY) / normY <= r;
    }
}
