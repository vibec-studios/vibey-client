package dev.henny.vibeyclient.module.visuals.particles.core;

import dev.henny.vibeyclient.module.visuals.core.ParticleCategory;
import dev.henny.vibeyclient.module.visuals.core.ParticleTable;
import dev.henny.vibeyclient.module.visuals.effects.core.Preset;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class ParticlePresets {
    private static final Set<String> HITS = Set.of("crit", "enchanted_hit");
    private static final Set<String> FLAMES = Set.of("flame", "soul_fire_flame", "small_flame", "copper_fire_flame");
    private static final Set<String> SPARKLE = Set.of("end_rod", "firework", "electric_spark", "happy_villager", "glow");

    private ParticlePresets() {}

    public static Map<String, ParticleStyle> styles(Preset preset, Iterable<String> ids) {
        Map<String, ParticleStyle> out = new TreeMap<>();
        for (String id : ids) {
            ParticleStyle s = style(preset, id);
            if (!s.vanilla()) out.put(id, s);
        }
        return out;
    }

    public static boolean matches(Preset preset, ParticleStyles current, Iterable<String> ids) {
        return styles(preset, ids).equals(current.all());
    }

    public static ParticleStyle style(Preset preset, String id) {
        String path = path(id);
        ParticleCategory c = ParticleTable.category(id);
        ParticleStyle v = ParticleStyle.VANILLA;
        return switch (preset) {
            case VANILLA -> v;
            case CLEAN -> switch (c) {
                case AMBIENT -> v.withAmount(40);
                case WEATHER -> v.withAmount(50);
                case BLOCK -> v.withAmount(60);
                case EFFECT -> v.withAmount(50);
                default -> path.equals("poof") || path.equals("large_smoke") ? v.withAmount(50) : v;
            };
            case PVP -> {
                if (HITS.contains(path)) yield v.withAmount(150).withSize(125);
                yield switch (c) {
                    case AMBIENT -> v.withAmount(50);
                    case WEATHER -> v.withAmount(30);
                    case BLOCK -> v.withAmount(40);
                    case EFFECT -> v.withAmount(30);
                    default -> v;
                };
            }
            case FANCY -> {
                if (HITS.contains(path)) yield v.withAmount(200).withSize(120);
                if (path.equals("totem_of_undying")) yield v.withAmount(200);
                if (FLAMES.contains(path) || SPARKLE.contains(path)) yield v.withAmount(150);
                if (path.equals("heart")) yield v.withSize(130);
                yield c == ParticleCategory.AMBIENT ? v.withAmount(130) : v;
            }
        };
    }

    private static String path(String id) {
        int colon = id.indexOf(':');
        if (colon < 0) return id;
        return id.startsWith("minecraft:") ? id.substring(colon + 1) : id;
    }
}
