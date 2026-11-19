package dev.henny.vibeyclient.module.pvp.mixin;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The texture behind the hurt tint, repainted by {@code HitColor}. */
@Mixin(OverlayTexture.class)
public interface OverlayTextureAccessor {
    @Accessor("texture")
    DynamicTexture vibey$texture();
}
