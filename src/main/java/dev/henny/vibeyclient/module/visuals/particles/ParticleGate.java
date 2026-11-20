package dev.henny.vibeyclient.module.visuals.particles;

import dev.henny.vibeyclient.module.visuals.VisualsModule;
import dev.henny.vibeyclient.module.visuals.core.ParticleCategory;
import dev.henny.vibeyclient.module.visuals.core.ParticleTable;
import dev.henny.vibeyclient.module.visuals.particles.core.ParticleStyle;
import dev.henny.vibeyclient.module.visuals.particles.core.StylePlan;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class ParticleGate {
    private static final double JITTER = 0.12;
    private static final Map<ParticleType<?>, ParticleCategory> CATEGORIES = new IdentityHashMap<>();
    private static int creating;
    private static boolean extra;
    private static long created, dropped, added;

    private ParticleGate() {}

    public static int plan(ParticleOptions options) {
        if (extra) return 1;
        VisualsModule m = VisualsModule.get();
        if (m == null || !m.isEnabled()) return 1;
        return count(m, StyleStore.slot(options.getType()));
    }

    public static void enter() {
        creating++;
        created++;
    }

    public static void exit() {
        if (creating > 0) creating--;
    }

    public static boolean dropDirect(Particle particle) {
        if (creating > 0 || !(particle instanceof TerrainParticle)) return false;
        VisualsModule m = VisualsModule.get();
        if (m == null || !m.isEnabled()) return false;
        StyleSlot slot = StyleStore.slot(ParticleTypes.BLOCK);
        ((StyledParticle) particle).vibey$slot(slot);
        return count(m, slot) == 0;
    }

    public static void tag(Particle particle, ParticleOptions options) {
        if (particle == null) return;
        VisualsModule m = VisualsModule.get();
        if (m == null || !m.isEnabled()) return;
        ((StyledParticle) particle).vibey$slot(StyleStore.slot(options.getType()));
    }

    private static int count(VisualsModule m, StyleSlot slot) {
        ParticleStyle s = slot.style();
        int global = m.particles.get();
        boolean ambientOff = m.ambientOff.on();
        if (StylePlan.neutral(global, ambientOff, s)) return 1;
        int n = StylePlan.count(slot.category(), global, m.keepImportant.on(), ambientOff, s, slot.budget());
        if (n == 0) dropped++;
        return n;
    }

    public static void spawnExtra(ParticleEngine engine, ParticleOptions o, double x, double y, double z,
                                  double vx, double vy, double vz, int count) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        extra = true;
        try {
            for (int i = 0; i < count; i++) {
                engine.createParticle(o, x + r.nextDouble(-JITTER, JITTER), y + r.nextDouble(-JITTER, JITTER),
                        z + r.nextDouble(-JITTER, JITTER), vx * r.nextDouble(0.85, 1.15), vy * r.nextDouble(0.85, 1.15),
                        vz * r.nextDouble(0.85, 1.15));
                added++;
            }
        } finally {
            extra = false;
        }
    }

    public static ParticleCategory category(ParticleType<?> type) {
        ParticleCategory c = CATEGORIES.get(type);
        if (c != null) return c;
        Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(type);
        c = ParticleTable.category(id == null ? null : id.toString());
        CATEGORIES.put(type, c);
        return c;
    }

    public static long[] stats() {
        return new long[] {created, dropped, added};
    }

    public static void resetStats() {
        created = dropped = added = 0;
    }
}
