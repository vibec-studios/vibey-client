package dev.henny.vibeyclient.module.clips.mixin;

import com.mojang.blaze3d.opengl.GlBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The GL name of a readback buffer, for {@code glGetBufferSubData} (Clips readback). */
@Mixin(GlBuffer.class)
public interface GlBufferAccessor {
    @Accessor("handle")
    int vibey$handle();
}
