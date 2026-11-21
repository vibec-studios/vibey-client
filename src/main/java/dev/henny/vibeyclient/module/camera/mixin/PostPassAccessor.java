package dev.henny.vibeyclient.module.camera.mixin;

import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
//? if >=1.21.11 && <26.1 {
import com.mojang.blaze3d.buffers.GpuBuffer;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;
//?}


@Mixin(PostPass.class)
public interface PostPassAccessor {
    //? if >=1.21.11 && <26.1 {
    @Accessor("customUniforms")
    Map<String, GpuBuffer> vibey$customUniforms();
    //?}
}
