package dev.henny.vibeyclient.module.visuals.mixin.items;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.henny.vibeyclient.module.visuals.items.swing.SwingRender;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
abstract class SwingDurationMixin {
    @ModifyReturnValue(method = "getCurrentSwingDuration", require = 0, at = @At("RETURN"))
    private int vibey$duration(int ticks) {
        return SwingRender.duration((LivingEntity) (Object) this, ticks);
    }
}
