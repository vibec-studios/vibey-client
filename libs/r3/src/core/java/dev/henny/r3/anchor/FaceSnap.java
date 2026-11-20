package dev.henny.r3.anchor;

import dev.henny.r3.math.Axes;
import dev.henny.r3.math.Face;
import dev.henny.r3.object.Transform;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class FaceSnap {
    public static final double DEFAULT_OFFSET = 0.02;
    public static final double DEFAULT_GRID = 0.125;
    private static final ThreadLocal<Vector3d[]> AXES = ThreadLocal.withInitial(
            () -> new Vector3d[] {new Vector3d(), new Vector3d(), new Vector3d()});

    private FaceSnap() {}

    public static Face upFor(Face face, float viewerYaw, int quarterTurns) {
        if (!face.vertical()) return Face.UP;
        Face facing = Face.horizontalFromYaw(viewerYaw);
        Face up = face == Face.UP ? facing : facing.opposite();
        for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) up = up.clockwise();
        return up;
    }

    public static Quaternionf orientation(Face normal, Face up, Quaternionf out) {
        Vector3d[] a = AXES.get();
        Vector3d n = a[2].set(normal.stepX(), normal.stepY(), normal.stepZ());
        Vector3d u = a[1].set(up.stepX(), up.stepY(), up.stepZ());
        Vector3d r = a[0].set(u).cross(n);
        return Axes.fromAxes(r, u, n, out);
    }

    public static Vector3d snapToGrid(Vector3d point, Face face, double grid) {
        if (grid <= 0) return point;
        int axis = face.axis();
        if (axis != 0) point.x = Math.round(point.x / grid) * grid;
        if (axis != 1) point.y = Math.round(point.y / grid) * grid;
        if (axis != 2) point.z = Math.round(point.z / grid) * grid;
        return point;
    }

    public static void pose(Vector3dc point, Face face, Face up, double offset, double grid, Transform out) {
        Vector3d p = out.position().set(point);
        snapToGrid(p, face, grid);
        p.add(face.stepX() * offset, face.stepY() * offset, face.stepZ() * offset);
        orientation(face, up, out.orientation());
    }
}
