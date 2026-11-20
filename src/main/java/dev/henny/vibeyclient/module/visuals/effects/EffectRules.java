package dev.henny.vibeyclient.module.visuals.effects;

import dev.henny.vibeyclient.api.ServerRules;

import java.util.List;

public final class EffectRules {
    public static final String FIRE = "visuals.fire";
    public static final String PUMPKIN = "visuals.pumpkin";
    public static final String TOTEM = "visuals.totem";
    public static final String PARTICLES = "visuals.particles";
    public static final List<String> ALL = List.of(FIRE, PUMPKIN, TOTEM, PARTICLES);

    private static int changes;

    private EffectRules() {}

    public static void declare() {
        for (String key : ALL) ServerRules.supports(key);
        ServerRules.listen(() -> changes++);
    }

    public static boolean blocked(String key) {
        return ServerRules.isDisabled(key);
    }

    public static int changes() {
        return changes;
    }
}
