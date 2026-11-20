package dev.henny.vibeyclient.module.visuals.mixin.items;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSetup.class)
public interface RenderSetupAccess {
    @Accessor("textureTransform")
    TextureTransform vibey$textureTransform();
}
