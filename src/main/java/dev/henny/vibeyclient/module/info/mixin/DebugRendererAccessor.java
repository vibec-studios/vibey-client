package dev.henny.vibeyclient.module.info.mixin;

import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
//? if <1.21.9 {
/*import org.spongepowered.asm.mixin.gen.Accessor;
*///?}

@Mixin(DebugRenderer.class)
public interface DebugRendererAccessor {
    //? if <1.21.9 {
    /*@Accessor("renderChunkborder")
    boolean vibey$chunkBorders();
    *///?}
}
