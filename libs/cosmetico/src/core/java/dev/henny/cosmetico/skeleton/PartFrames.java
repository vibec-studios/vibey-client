package dev.henny.cosmetico.skeleton;

import dev.henny.cosmetico.model.PlayerBone;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public interface PartFrames {
    void apply(PlayerBone part, Matrix4f pose, Matrix3f normal);
}
