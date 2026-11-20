package dev.henny.cosmetico.mc.fx;

import dev.henny.cosmetico.fx.SpritePools;

import java.util.Random;
import java.util.UUID;

public final class Sprites {
    private Sprites() {}

    private static final Random RANDOM = new Random();
    private static final SpritePools<UUID> POOLS = new SpritePools<>(RANDOM);

    public static SpritePools<UUID> pools() {
        return POOLS;
    }

    public static Random random() {
        return RANDOM;
    }
}
