package dev.henny.vibeyclient.module.camera.mixin;

import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
//? if >=1.21.11 && <26.1 {
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;
//?}


@Mixin(PostChain.class)
public interface PostChainAccessor {
    //? if >=1.21.11 && <26.1 {
    @Accessor("passes")
    List<PostPass> vibey$passes();
    //?}
}
