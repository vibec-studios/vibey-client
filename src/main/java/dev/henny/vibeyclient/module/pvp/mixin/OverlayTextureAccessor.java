package dev.henny.vibeyclient.module.pvp.mixin;

import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
//? if >=1.21.11 && <26.1 {
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.gen.Accessor;
//?}

/** The texture behind the hurt tint, repainted by {@code HitColor}. */
@Mixin(OverlayTexture.class)
public interface OverlayTextureAccessor {
    //? if >=1.21.11 && <26.1 {
    @Accessor("texture")
    DynamicTexture vibey$texture();
    //?}
}
