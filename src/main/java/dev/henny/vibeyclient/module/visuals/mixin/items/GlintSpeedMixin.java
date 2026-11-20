package dev.henny.vibeyclient.module.visuals.mixin.items;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.henny.vibeyclient.module.visuals.items.glint.GlintRender;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextureTransform.class)
abstract class GlintSpeedMixin {
    @WrapOperation(method = "setupGlintTexturing", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private static Object vibey$speed(OptionInstance<?> option, Operation<Object> original) {
        Object v = original.call(option);
        return v instanceof Double d ? Double.valueOf(GlintRender.speed(d)) : v;
    }
}
