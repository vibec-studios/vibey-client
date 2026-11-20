package dev.henny.vibeyclient.module.visuals.particles;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class ParticleIds {
    private static List<String> cache;

    private ParticleIds() {}

    public static List<String> all() {
        if (cache != null && cache.size() == BuiltInRegistries.PARTICLE_TYPE.size()) return cache;
        List<String> out = new ArrayList<>();
        for (Identifier id : BuiltInRegistries.PARTICLE_TYPE.keySet()) out.add(id.toString());
        out.sort(null);
        cache = List.copyOf(out);
        return cache;
    }

    public static ParticleType<?> type(String id) {
        Identifier key = Identifier.tryParse(id);
        return key == null ? null : BuiltInRegistries.PARTICLE_TYPE.getValue(key);
    }
}
