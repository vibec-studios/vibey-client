package dev.henny.vibeyclient.avatar.render;

import dev.henny.vibeyclient.avatar.AvatarState;

/**
 * Implemented by the player render state (1.21.2+) through a mixin: carries the avatar from extractRenderState to
 * setupAnim/the layers, which only see the render state.
 */
public interface AvatarCarrier {
    AvatarState vibey$avatar();

    void vibey$setAvatar(AvatarState state);
}
