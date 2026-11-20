package dev.henny.companio.sense;

import java.util.List;

public final class Variables {
    private Variables() {}

    public static final List<String> BUILTIN = List.of(
            "weather.rain", "weather.snow", "weather.thunder", "weather.clear", "self.wet",
            "time.day", "time.night", "time.dawn", "time.dusk", "time.hour",
            "world.dimension", "world.biome", "world.cold", "world.hot", "world.light", "world.sky",
            "player.sneaking", "player.sprinting", "player.swimming", "player.elytra", "player.flying", "player.sleeping",
            "player.eating", "player.onGround", "player.inWater", "player.riding", "player.hurt", "player.health",
            "player.idle", "player.speed", "player.distance", "player.looking", "player.held", "player.offhand",
            "self.speed", "self.moving", "self.onGround", "self.inWater", "self.onIce", "self.onSnow", "self.held",
            "self.riding", "self.command", "self.behavior", "self.time", "self.age", "self.size", "self.variant",
            "near.water", "near.lava", "near.fire", "near.mob", "near.player", "near.companion");

    public static final List<String> EVENTS = List.of(
            "spawn", "pet", "pick_up", "place", "rename", "command_sit", "command_follow", "command_stay",
            "owner_hurt", "owner_low_health", "owner_eat", "owner_jump", "owner_sleep", "owner_wake", "owner_look",
            "item_change", "rain_start", "rain_stop", "thunder_start", "night_start", "day_start",
            "enter_water", "leave_water", "land", "teleport", "companion_near");
}
