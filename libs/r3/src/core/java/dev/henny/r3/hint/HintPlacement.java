package dev.henny.r3.hint;

import dev.henny.r3.object.Transform;
import dev.henny.r3.view.CameraState;
import dev.henny.r3.view.ScreenProjection;
import org.joml.Vector3d;

public final class HintPlacement {
    private static final double[][] CORNERS = {{-0.5, -0.5}, {0.5, -0.5}, {0.5, 0.5}, {-0.5, 0.5}};

    private final Vector3d right = new Vector3d();
    private final Vector3d up = new Vector3d();
    private final double[] screen = new double[2];

    public double bottomOf(Transform t, CameraState camera, double verticalFovDegrees, double aspect) {
        t.right(right).mul(t.worldWidth());
        t.up(up).mul(t.worldHeight());
        double lowest = -1;
        for (double[] c : CORNERS) {
            double x = t.position().x + right.x * c[0] + up.x * c[1];
            double y = t.position().y + right.y * c[0] + up.y * c[1];
            double z = t.position().z + right.z * c[0] + up.z * c[1];
            if (!ScreenProjection.project(camera, verticalFovDegrees, aspect, x, y, z, screen)) return Double.NaN;
            lowest = Math.max(lowest, screen[1]);
        }
        return lowest;
    }

    public static int lineY(int crosshairY, int screenHeight, int lineHeight, double targetBottom, int gap, int bottomMargin) {
        int below = crosshairY + gap;
        if (Double.isNaN(targetBottom)) return below;
        int under = (int) Math.ceil((targetBottom + 1) / 2 * screenHeight) + gap;
        if (under <= below) return below;
        return under + lineHeight <= screenHeight - bottomMargin ? under : below;
    }
}
