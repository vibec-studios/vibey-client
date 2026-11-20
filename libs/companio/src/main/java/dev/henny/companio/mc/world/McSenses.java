package dev.henny.companio.mc.world;

import dev.henny.companio.move.BlockView;
import dev.henny.companio.sense.Senses;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;

public final class McSenses {
    private final OwnerTracker owner = new OwnerTracker();
    private final NearScan near = new NearScan();
    private final MusicWatch music = new MusicWatch();
    private int ticks;

    public void fill(ClientLevel level, Player p, BlockView view, double cx, double cy, double cz, boolean looking,
                     double nearCompanion, Senses s) {
        owner.fill(p, s.owner, looking);
        OwnerExtras.fill(p, s.owner);
        s.music = music.near(cx, cy, cz);
        BlockPos at = BlockPos.containing(cx, cy + 0.3, cz);
        s.dayTime = level.getDayTime();
        s.dimension = level.dimension().identifier().toString();
        Holder<Biome> biome = level.getBiome(at);
        s.biome = biome.unwrapKey().map(k -> k.identifier().toString()).orElse("minecraft:plains");
        Biome b = biome.value();
        s.cold = b.coldEnoughToSnow(at, level.getSeaLevel());
        s.hot = b.getBaseTemperature() > 1.0f;
        s.sky = level.canSeeSky(at);
        s.raining = level.isRaining() && b.hasPrecipitation();
        s.thundering = level.isThundering();
        s.light = Math.max(level.getBrightness(LightLayer.BLOCK, at), level.isBrightOutside() ? level.getBrightness(LightLayer.SKY, at) : 0);
        s.nearCompanion = nearCompanion;
        if (ticks++ % 10 == 0) near.scan(level, p, view, cx, cy, cz, s);
    }
}
