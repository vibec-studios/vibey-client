package dev.henny.companio.sense;

import java.util.function.Consumer;

public final class EventDetector {
    private boolean first = true;
    private boolean rain, thunder, night, hurt, low, eating, sleeping, looking, inWater, nearCompanion, onGround = true;
    private boolean advancement, music;
    private int level;
    private String held = "", target = "minecraft:air";
    private double fallFrom;

    public void detect(Senses s, boolean selfInWater, boolean selfOnGround, double selfY, Consumer<String> fire) {
        Owner o = s.owner;
        boolean isNight = s.night(), isLow = o.health < 0.3f, near = s.nearCompanion < 4;
        if (!first) {
            edge(rain, s.raining, "rain_start", "rain_stop", fire);
            edge(thunder, s.thundering, "thunder_start", null, fire);
            edge(night, isNight, "night_start", "day_start", fire);
            edge(hurt, o.hurt, "owner_hurt", null, fire);
            edge(low, isLow, "owner_low_health", null, fire);
            edge(eating, o.eating, "owner_eat", null, fire);
            edge(sleeping, o.sleeping, "owner_sleep", "owner_wake", fire);
            edge(looking, o.looking, "owner_look", null, fire);
            edge(inWater, selfInWater, "enter_water", "leave_water", fire);
            edge(nearCompanion, near, "companion_near", null, fire);
            edge(advancement, o.advancement, "owner_advancement", null, fire);
            edge(music, s.music, "music_start", "music_stop", fire);
            if (o.level > level) fire.accept("owner_level_up");
            if (!target.equals(o.target) && !"minecraft:air".equals(o.target)) fire.accept("target_change");
            if (o.jumped) fire.accept("owner_jump");
            if (!held.equals(o.held)) fire.accept("item_change");
            if (!onGround && selfOnGround && fallFrom - selfY > 1.0) fire.accept("land");
        }
        if (onGround && !selfOnGround) fallFrom = selfY;
        if (!selfOnGround && selfY > fallFrom) fallFrom = selfY;
        first = false;
        rain = s.raining;
        thunder = s.thundering;
        night = isNight;
        hurt = o.hurt;
        low = isLow;
        eating = o.eating;
        sleeping = o.sleeping;
        looking = o.looking;
        inWater = selfInWater;
        nearCompanion = near;
        held = o.held;
        target = o.target;
        level = o.level;
        advancement = o.advancement;
        music = s.music;
        onGround = selfOnGround;
    }

    private static void edge(boolean before, boolean now, String on, String off, Consumer<String> fire) {
        if (!before && now && on != null) fire.accept(on);
        if (before && !now && off != null) fire.accept(off);
    }
}
