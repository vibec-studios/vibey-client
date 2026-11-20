package dev.henny.cosmetico.mc.render;

import dev.henny.cosmetico.model.Locator;
import dev.henny.cosmetico.skeleton.LocatorMath;
import dev.henny.cosmetico.skeleton.LocatorSink;
import dev.henny.cosmetico.state.Avatar;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class LocatorCollector implements LocatorSink {
    private final Vector3f v = new Vector3f();
    private double camX = Double.NaN, camY, camZ;
    private Avatar target;

    public void camera(double x, double y, double z) {
        camX = x;
        camY = y;
        camZ = z;
    }

    public void target(Avatar a) {
        target = a;
    }

    @Override
    public void locator(Locator l, Matrix4f boneMatrix, float[] bonePivotM) {
        if (target == null || Double.isNaN(camX)) return;
        LocatorMath.local(l, bonePivotM, v);
        boneMatrix.transformPosition(v);
        target.locators.set(l.name(), v.x + camX, v.y + camY, v.z + camZ);
    }
}
