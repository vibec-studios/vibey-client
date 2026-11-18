package dev.henny.vibeyclient.avatar.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.avatar.bundle.Bundle.PlayerBone;

/** Applies the current transform of a vanilla player part (head, body, arms, legs) to a pose stack in model space. */
public interface PlayerParts {
    void apply(PlayerBone part, PoseStack pose);
}
