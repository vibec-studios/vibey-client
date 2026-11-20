package dev.henny.vibeyclient.module.visuals.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ParticleTable {
    private static final String VANILLA = "minecraft:";
    private static final Map<String, ParticleCategory> TABLE = new HashMap<>();

    static {
        put(ParticleCategory.IMPORTANT, List.of("crit", "enchanted_hit", "damage_indicator", "sweep_attack",
                "totem_of_undying", "explosion", "explosion_emitter", "sonic_boom", "flash", "elder_guardian",
                "block_marker", "vibration", "shriek", "trial_omen", "raid_omen", "ominous_spawning",
                "trial_spawner_detection", "trial_spawner_detection_ominous", "gust", "small_gust",
                "gust_emitter_large", "gust_emitter_small"));
        put(ParticleCategory.AMBIENT, List.of("ash", "white_ash", "crimson_spore", "warped_spore", "spore_blossom_air",
                "falling_spore_blossom", "mycelium", "underwater", "portal", "reverse_portal", "enchant", "nautilus",
                "cherry_leaves", "pale_oak_leaves", "tinted_leaves", "firefly", "campfire_cosy_smoke",
                "campfire_signal_smoke", "dolphin", "current_down", "bubble_column_up", "bubble", "bubble_pop", "glow",
                "egg_crack", "falling_nectar", "smoke", "flame", "soul_fire_flame", "small_flame", "copper_fire_flame",
                "lava", "sculk_soul", "sculk_charge", "sculk_charge_pop"));
        put(ParticleCategory.WEATHER, List.of("rain", "splash", "snowflake", "dripping_water", "falling_water",
                "dripping_lava", "falling_lava", "landing_lava", "dripping_honey", "falling_honey", "landing_honey",
                "dripping_obsidian_tear", "falling_obsidian_tear", "landing_obsidian_tear", "dripping_dripstone_lava",
                "falling_dripstone_lava", "dripping_dripstone_water", "falling_dripstone_water"));
        put(ParticleCategory.BLOCK, List.of("block", "block_crumble", "falling_dust", "dust_pillar", "dust_plume", "item",
                "item_slime", "item_cobweb", "item_snowball", "scrape", "wax_on", "wax_off"));
        put(ParticleCategory.EFFECT, List.of("effect", "entity_effect", "instant_effect", "witch", "dragon_breath",
                "infested"));
    }

    private ParticleTable() {}

    public static ParticleCategory category(String id) {
        if (id == null) return ParticleCategory.OTHER;
        String key = id.startsWith(VANILLA) ? id.substring(VANILLA.length()) : id;
        return TABLE.getOrDefault(key, ParticleCategory.OTHER);
    }

    public static int size() {
        return TABLE.size();
    }

    private static void put(ParticleCategory c, List<String> ids) {
        for (String id : ids) TABLE.put(id, c);
    }
}
