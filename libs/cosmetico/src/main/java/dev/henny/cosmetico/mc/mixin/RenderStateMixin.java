package dev.henny.cosmetico.mc.mixin;

import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.mc.render.AvatarCarrier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

//? if >=1.21.9 {
@Mixin(net.minecraft.client.renderer.entity.state.AvatarRenderState.class)
//?} else if >=1.21.2 {
/*@Mixin(net.minecraft.client.renderer.entity.state.PlayerRenderState.class)
*///?} else
/*@Mixin(net.minecraft.client.player.AbstractClientPlayer.class)*/
public abstract class RenderStateMixin implements AvatarCarrier {
    @Unique
    private Avatar cosmetico$avatarState;

    @Override
    public Avatar cosmetico$avatar() {
        return cosmetico$avatarState;
    }

    @Override
    public void cosmetico$setAvatar(Avatar avatar) {
        cosmetico$avatarState = avatar;
    }
}
