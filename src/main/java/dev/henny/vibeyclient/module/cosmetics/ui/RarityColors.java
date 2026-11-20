package dev.henny.vibeyclient.module.cosmetics.ui;

import dev.henny.cosmetico.model.Rarity;

public final class RarityColors {
    private RarityColors() {}

    public static int of(Rarity r) {
        return switch (r == null ? Rarity.COMMON : r) {
            case COMMON -> 0xFFA1A1AA;
            case RARE -> 0xFF3FE0C5;
            case EPIC -> 0xFF8B6CFF;
            case LEGENDARY -> 0xFFFFB547;
        };
    }
}
