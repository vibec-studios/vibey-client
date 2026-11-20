package dev.henny.vibeyclient.module.info.light;

import dev.henny.vibeyclient.module.info.core.SpawnLight;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.dimension.DimensionType;

public final class LightProbe {
    private static int blockLight;
    private static int color;

    private LightProbe() {}

    public static void read(ClientLevel level, BlockPos target, Direction face) {
        BlockPos at = target.relative(face);
        blockLight = level.getBrightness(LightLayer.BLOCK, at);
        color = Theme.TEXT;
        if (face != Direction.UP || !level.getBlockState(target).isFaceSturdy(level, target, Direction.UP)) return;
        DimensionType dim = level.dimensionType();
        int sky = level.getBrightness(LightLayer.SKY, at);
        color = SpawnLight.classify(blockLight, sky, dim.monsterSpawnBlockLightLimit(),
                dim.monsterSpawnLightTest().getMaxValue()).color();
    }

    public static int blockLight() {
        return blockLight;
    }

    public static int color() {
        return color;
    }
}
