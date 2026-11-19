package dev.henny.cosmetico.fx;

import dev.henny.cosmetico.fixtures.Fixtures;
import dev.henny.cosmetico.model.Bundle;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpritePoolTest {
    private static Bundle bundle() {
        var o = Fixtures.cosmetic("t:s", "aura");
        o.add("sprites", Fixtures.json("{\"a\":[{\"id\":\"spark\",\"texture\":\"main\",\"uv\":[0,0,8,8]}]}").get("a"));
        return Fixtures.parse(o);
    }

    @Test
    void spawnsTicksAndExpires() {
        SpritePool p = new SpritePool(new Random(1));
        Bundle b = bundle();
        p.spawn(b, new SpriteSpawn(0, 0, 0, 0, 0, 0.1f, 0, 0, 0, 0, 3, 0.1f, 0.2f, 0.01f, true));
        assertEquals(3, p.count);
        p.tick();
        assertTrue(p.y[0] > 0.08);
        for (int i = 0; i < 10; i++) p.tick();
        assertEquals(0, p.count);
    }

    @Test
    void poolIsCapped() {
        SpritePool p = new SpritePool(new Random(1));
        p.spawn(bundle(), new SpriteSpawn(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 300, 1, 0.2f, 0, false));
        assertEquals(SpritePool.MAX, p.count);
    }

    @Test
    void quadsFaceTheCamera() {
        SpritePool p = new SpritePool(new Random(1));
        p.spawn(bundle(), new SpriteSpawn(0, 1, 2, 3, 0, 0, 0, 0, 0, 0, 1, 1, 0.5f, 0, false));
        float[] d = new float[32];
        int end = SpriteQuads.quad(d, 0, p, 0, 0f, 1, 2, 3, new Vector3f(1, 0, 0), new Vector3f(0, 1, 0));
        assertEquals(32, end);
        assertEquals(-0.25f, d[0], 1e-6f);
        assertEquals(-0.25f, d[1], 1e-6f);
        assertEquals(0.5f, d[4], 1e-6f);
        assertEquals(0.5f, d[8 * 2 + 3], 1e-6f);
    }
}
