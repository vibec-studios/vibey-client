package dev.henny.vibeyclient.module.cosmetics.mixin;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** The player renderers by skin model (GUI preview without a player entity, 1.21.2 - 1.21.5). */
@Mixin(EntityRenderDispatcher.class)
public interface EntityRenderDispatcherAccessor {
    @SuppressWarnings("rawtypes")
    @Accessor("playerRenderers")
    Map vibey$playerRenderers();
}
