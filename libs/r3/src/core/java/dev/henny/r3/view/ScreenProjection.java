package dev.henny.r3.view;

import org.joml.Vector3dc;

public final class ScreenProjection {
    private ScreenProjection() {}

    public static boolean project(CameraState camera, double verticalFovDegrees, double aspect,
                                  double x, double y, double z, double[] out) {
        Vector3dc eye = camera.position();
        double dx = x - eye.x(), dy = y - eye.y(), dz = z - eye.z();
        double forward = dx * camera.look().x() + dy * camera.look().y() + dz * camera.look().z();
        if (forward <= 1e-4) return false;
        double side = dx * camera.right().x() + dy * camera.right().y() + dz * camera.right().z();
        double up = dx * camera.up().x() + dy * camera.up().y() + dz * camera.up().z();
        double t = Math.tan(Math.toRadians(verticalFovDegrees) / 2);
        out[0] = side / (forward * t * aspect);
        out[1] = -up / (forward * t);
        return true;
    }
}
