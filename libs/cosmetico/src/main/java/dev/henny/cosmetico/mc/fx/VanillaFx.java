package dev.henny.cosmetico.mc.fx;

import dev.henny.cosmetico.fx.FxSink;
import dev.henny.cosmetico.fx.SpriteSpawn;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Sound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.UUID;

public final class VanillaFx implements FxSink {
    public static final VanillaFx INSTANCE = new VanillaFx();
    private static final RandomSource RANDOM = RandomSource.create();

    private VanillaFx() {}

    public static RandomSource random() {
        return RANDOM;
    }

    @Override
    public void particle(String id, double x, double y, double z, float vx, float vy, float vz, int count, float sx, float sy, float sz) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ParticleOptions opt = VanillaIds.particle(id);
        if (opt == null) return;
        for (int i = 0; i < count; i++) {
            double px = x + (RANDOM.nextFloat() * 2 - 1) * sx;
            double py = y + (RANDOM.nextFloat() * 2 - 1) * sy;
            double pz = z + (RANDOM.nextFloat() * 2 - 1) * sz;
            mc.level.addParticle(opt, px, py, pz, vx, vy, vz);
        }
    }

    @Override
    public void sound(String id, double x, double y, double z, float volume, float pitch) {
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(VanillaIds.sound(id), SoundSource.PLAYERS, volume, pitch, RANDOM, x, y, z));
    }

    @Override
    public void embedded(Sound sound, double x, double y, double z, float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        float game = mc.options.getSoundSourceVolume(SoundSource.MASTER) * mc.options.getSoundSourceVolume(SoundSource.PLAYERS);
        EmbeddedSounds.play(sound, x, y, z, volume * game, pitch);
    }

    @Override
    public void sprite(UUID owner, Bundle bundle, SpriteSpawn spawn) {
        if (Minecraft.getInstance().level == null) return;
        Sprites.pools().pool(owner).spawn(bundle, spawn);
    }
}
