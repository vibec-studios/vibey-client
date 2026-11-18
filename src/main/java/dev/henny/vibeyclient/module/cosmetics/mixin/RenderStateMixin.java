package dev.henny.vibeyclient.module.cosmetics.mixin;

import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.render.AvatarCarrier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Lets the player render state carry the avatar (1.21.2+). On 1.21.1 there is no render state: an empty carrier. */
//? if >=1.21.9 {
@Mixin(net.minecraft.client.renderer.entity.state.AvatarRenderState.class)
//?} else if >=1.21.2 {
/*@Mixin(net.minecraft.client.renderer.entity.state.PlayerRenderState.class)
*///?} else
/*@Mixin(net.minecraft.client.player.AbstractClientPlayer.class)*/
public abstract class RenderStateMixin implements AvatarCarrier {
    @Unique
    private AvatarState vibey$avatar;

    @Override
    public AvatarState vibey$avatar() {
        return vibey$avatar;
    }

    @Override
    public void vibey$setAvatar(AvatarState state) {
        vibey$avatar = state;
    }
}
