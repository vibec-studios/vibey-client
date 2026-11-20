package dev.henny.r3.mc.world;

import dev.henny.r3.anchor.BlockHit;
import dev.henny.r3.anchor.EntityPose;
import dev.henny.r3.anchor.WorldQuery;
import dev.henny.r3.mc.compat.Game;
import dev.henny.r3.render.Light;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import org.joml.Vector3dc;

import java.util.UUID;

public final class McWorldQuery implements WorldQuery {
    public static final String NO_WORLD = "r3:none";

    @Override
    public String dimension() {
        ClientLevel level = Game.level();
        return level == null ? NO_WORLD : Game.dimension(level);
    }

    @Override
    public boolean raycastBlocks(Vector3dc origin, Vector3dc direction, double maxDistance, BlockHit out) {
        ClientLevel level = Game.level();
        return level != null && BlockRaycast.cast(level, Game.mc().player, origin, direction, maxDistance, out);
    }

    @Override
    public boolean entity(UUID id, EntityPose out) {
        ClientLevel level = Game.level();
        return level != null && EntityLookup.pose(level, id, Game.partialTick(), out);
    }

    @Override
    public int lightAt(double x, double y, double z) {
        ClientLevel level = Game.level();
        if (level == null) return Light.FULL_BRIGHT;
        BlockPos pos = BlockPos.containing(x, y, z);
        return Light.pack(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos));
    }
}
