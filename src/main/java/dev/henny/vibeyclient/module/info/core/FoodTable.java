package dev.henny.vibeyclient.module.info.core;

import java.util.Map;
import java.util.Set;

public final class FoodTable {
    private static final Map<String, FoodValue> WITHOUT_COMPONENT = Map.of(
            "minecraft:cake", new FoodValue(2, FoodMath.saturationOf(2, 0.1f)));
    private static final Set<String> RISKY = Set.of("minecraft:rotten_flesh", "minecraft:chicken",
            "minecraft:pufferfish", "minecraft:spider_eye", "minecraft:poisonous_potato", "minecraft:suspicious_stew");

    private FoodTable() {}

    public static FoodValue withoutComponent(String itemId) {
        return WITHOUT_COMPONENT.get(itemId);
    }

    public static boolean risky(String itemId) {
        return RISKY.contains(itemId);
    }
}
