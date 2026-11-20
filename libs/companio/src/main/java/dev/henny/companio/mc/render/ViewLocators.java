package dev.henny.companio.mc.render;

import dev.henny.companio.world.CompanionView;
import dev.henny.cosmetico.model.Locator;
import dev.henny.cosmetico.skeleton.LocatorMath;
import dev.henny.cosmetico.skeleton.LocatorSink;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class ViewLocators implements LocatorSink {
    private static final Map<CompanionView, Map<String, double[]>> POSITIONS = new WeakHashMap<>();
    private final Vector3f v = new Vector3f();
    private Map<String, double[]> target;
    private double camX, camY, camZ;

    ViewLocators begin(CompanionView view, double camX, double camY, double camZ) {
        this.target = POSITIONS.computeIfAbsent(view, k -> new HashMap<>());
        this.camX = camX;
        this.camY = camY;
        this.camZ = camZ;
        return this;
    }

    @Override
    public void locator(Locator l, Matrix4f boneMatrix, float[] bonePivotM) {
        if (target == null) return;
        LocatorMath.local(l, bonePivotM, v);
        boneMatrix.transformPosition(v);
        double[] p = target.computeIfAbsent(l.name(), k -> new double[3]);
        p[0] = v.x + camX;
        p[1] = v.y + camY;
        p[2] = v.z + camZ;
    }

    public static double[] get(CompanionView view, String locator) {
        if (locator == null) return null;
        Map<String, double[]> m = POSITIONS.get(view);
        return m == null ? null : m.get(locator);
    }
}
