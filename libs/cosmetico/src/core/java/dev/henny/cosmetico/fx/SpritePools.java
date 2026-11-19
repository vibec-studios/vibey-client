package dev.henny.cosmetico.fx;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public final class SpritePools<K> {
    private final Map<K, SpritePool> pools = new IdentityHashMap<>();
    private final Random random;

    public SpritePools(Random random) {
        this.random = random;
    }

    public SpritePool pool(K owner) {
        return pools.computeIfAbsent(owner, k -> new SpritePool(random));
    }

    public SpritePool peek(K owner) {
        return pools.get(owner);
    }

    public void clear(K owner) {
        pools.remove(owner);
    }

    public void tick() {
        for (Iterator<SpritePool> it = pools.values().iterator(); it.hasNext(); ) {
            SpritePool p = it.next();
            p.tick();
            if (p.count == 0) it.remove();
        }
    }
}
