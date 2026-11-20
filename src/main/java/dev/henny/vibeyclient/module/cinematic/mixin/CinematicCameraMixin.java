package dev.henny.vibeyclient.module.cinematic.mixin;

import dev.henny.vibeyclient.module.cinematic.run.CameraDriver;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class CinematicCameraMixin implements CameraDriver.Target {
    @Shadow private boolean detached;

    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("TAIL"))
    private void vibey$cinematic(Level level, Entity entity, boolean thirdPerson, boolean mirror, float partial, CallbackInfo ci) {
        CameraDriver.afterSetup((Camera) (Object) this, partial, this);
    }

    @Override
    public void vibey$place(double x, double y, double z, float yaw, float pitch, boolean away) {
        setRotation(yaw, pitch);
        setPosition(x, y, z);
        detached = away;
    }
}
