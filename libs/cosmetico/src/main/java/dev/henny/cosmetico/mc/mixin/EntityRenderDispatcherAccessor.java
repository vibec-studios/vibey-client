package dev.henny.cosmetico.mc.mixin;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public interface EntityRenderDispatcherAccessor {
    @SuppressWarnings("rawtypes")
    @Accessor("playerRenderers")
    Map cosmetico$playerRenderers();
}
