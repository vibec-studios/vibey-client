package dev.henny.vibeyclient.module.lyrics.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
//? if >=1.21.9 <1.21.11 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.module.lyrics.render.WorldLyrics;
import dev.henny.vibeyclient.module.lyrics.render.WorldSink;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

/**
 * Only does something on Minecraft 1.21.9/1.21.10: Fabric API has no world render events there, so the lyrics hook
 * into the moment the game collects entity submits (the same spot Fabric's BEFORE_ENTITIES uses from 1.21.11 on).
 * On every other version this mixin is empty (one mixin config for all versions).
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    //? if >=1.21.9 <1.21.11 {
    /*@Inject(method = "submitEntities", at = @At("HEAD"))
    private void vibeyclient$lyrics$submitLyrics(PoseStack poseStack, LevelRenderState state, SubmitNodeCollector collector, CallbackInfo ci) {
        WorldLyrics.render(poseStack, new WorldSink(collector));
    }
    *///?}
}
