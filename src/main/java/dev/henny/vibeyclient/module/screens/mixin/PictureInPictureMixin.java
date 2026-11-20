package dev.henny.vibeyclient.module.screens.mixin;

import dev.henny.vibeyclient.module.screens.hook.PipPose;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiGraphics.class)
abstract class PictureInPictureMixin {
    private static final String ENTITY = "submitEntityRenderState";
    private static final String BOOK = "submitBookModelRenderState";
    private static final String BANNER = "submitBannerPatternRenderState";
    private static final String SIGN = "submitSignRenderState";
    private static final String SKIN = "submitSkinRenderState";

    @ModifyVariable(method = {ENTITY, BOOK, BANNER, SIGN, SKIN}, at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int vibey$x0(int v) {
        return PipPose.x((GuiGraphics) (Object) this, v);
    }

    @ModifyVariable(method = {ENTITY, BOOK, BANNER, SIGN, SKIN}, at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private int vibey$y0(int v) {
        return PipPose.y((GuiGraphics) (Object) this, v);
    }

    @ModifyVariable(method = {ENTITY, BOOK, BANNER, SIGN, SKIN}, at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0)
    private int vibey$x1(int v) {
        return PipPose.x((GuiGraphics) (Object) this, v);
    }

    @ModifyVariable(method = {ENTITY, BOOK, BANNER, SIGN, SKIN}, at = @At("HEAD"), argsOnly = true, ordinal = 3, require = 0)
    private int vibey$y1(int v) {
        return PipPose.y((GuiGraphics) (Object) this, v);
    }

    @ModifyVariable(method = {ENTITY, BOOK, SIGN, SKIN}, at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private float vibey$scale(float v) {
        return PipPose.scale((GuiGraphics) (Object) this, v);
    }
}
