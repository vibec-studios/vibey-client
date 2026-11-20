package dev.henny.r3.mc.view;

import dev.henny.r3.mc.compat.Game;
import dev.henny.r3.view.CameraFrustum;
import dev.henny.r3.view.CameraState;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

public final class CameraReader {
    public static final double FRUSTUM_MARGIN_DEGREES = 12;

    private CameraReader() {}

    public static boolean read(CameraState out) {
        //? if >=26.2 {
        /*Camera camera = Game.mc().gameRenderer.mainCamera();
        *///?} else
        Camera camera = Game.mc().gameRenderer.getMainCamera();
        if (camera == null || !camera.isInitialized()) return false;
        //? if >=1.21.11 {
        Vec3 p = camera.position();
        out.set(p.x, p.y, p.z, camera.yRot(), camera.xRot());
        //?} else {
        /*Vec3 p = camera.getPosition();
        out.set(p.x, p.y, p.z, camera.getYRot(), camera.getXRot());
        *///?}
        return true;
    }

    public static CameraFrustum frustum(CameraState camera, CameraFrustum out) {
        return out.set(camera, Game.fovDegrees(), Game.aspect(), FRUSTUM_MARGIN_DEGREES);
    }
}
