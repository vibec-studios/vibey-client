package dev.henny.vibeyclient.module.camera.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.henny.vibeyclient.module.camera.look.CameraState;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
abstract class CameraMixin {
    @WrapOperation(method = "setup", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float vibey$yaw(Entity entity, float partialTick, Operation<Float> original) {
        return CameraState.yaw(entity, original.call(entity, partialTick));
    }

    @WrapOperation(method = "setup", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float vibey$pitch(Entity entity, float partialTick, Operation<Float> original) {
        return CameraState.pitch(entity, original.call(entity, partialTick));
    }

    @ModifyArg(method = "setup", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
    private float vibey$distance(float distance) {
        return CameraState.distance(((CameraAccessor) this).vibey$entity(), distance);
    }
}
