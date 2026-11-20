package dev.henny.cosmetico.mc.fx;

import dev.henny.cosmetico.mc.CosmeticoMod;
import dev.henny.cosmetico.mc.compat.McIds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class VanillaIds {
    private VanillaIds() {}

    private static final Map<String, ParticleOptions> PARTICLES = new HashMap<>();
    private static final Set<String> BAD_PARTICLES = new HashSet<>();
    private static final Map<String, SoundEvent> SOUNDS = new HashMap<>();

    static ParticleOptions particle(String id) {
        ParticleOptions o = PARTICLES.get(id);
        if (o != null || BAD_PARTICLES.contains(id)) return o;
        var type = BuiltInRegistries.PARTICLE_TYPE.getOptional(McIds.parse(id)).orElse(null);
        if (type instanceof ParticleOptions po) {
            PARTICLES.put(id, po);
            return po;
        }
        BAD_PARTICLES.add(id);
        CosmeticoMod.LOG.info("Particle {} is unknown or needs options; skipped", id);
        return null;
    }

    static SoundEvent sound(String id) {
        return SOUNDS.computeIfAbsent(id, k -> SoundEvent.createVariableRangeEvent(McIds.parse(k)));
    }
}
