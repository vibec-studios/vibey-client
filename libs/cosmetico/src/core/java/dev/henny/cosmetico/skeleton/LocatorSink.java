package dev.henny.cosmetico.skeleton;

import dev.henny.cosmetico.model.Locator;
import org.joml.Matrix4f;

public interface LocatorSink {
    void locator(Locator locator, Matrix4f boneMatrix, float[] bonePivotM);
}
