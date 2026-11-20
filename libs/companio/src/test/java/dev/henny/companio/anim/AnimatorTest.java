package dev.henny.companio.anim;

import dev.henny.companio.def.AnimRef;
import dev.henny.companio.def.Blend;
import dev.henny.companio.support.TestPacks;
import dev.henny.cosmetico.skeleton.Mix;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnimatorTest {
    private static float weight(Mix m, String name) {
        float w = 0;
        for (int i = 0; i < m.count; i++) if (m.anims[i].name.equals(name)) w += m.weights[i];
        return w;
    }

    @Test
    void crossFadesBaseAnimations() {
        Animator a = new Animator(TestPacks.blob("[]").model(null));
        Mix m = new Mix();
        for (int i = 0; i < 10; i++) a.base("idle", 1f, 0.05f);
        a.base("walk", 1f, 0.05f);
        a.fill(m);
        assertEquals(1f, weight(m, "idle") + weight(m, "walk"), 1e-4);
        assertTrue(weight(m, "idle") > 0.5f);
        for (int i = 0; i < 5; i++) a.base("walk", 1f, 0.05f);
        a.fill(m);
        assertEquals(1f, weight(m, "walk"), 1e-4);
    }

    @Test
    void layersAddOrReplace() {
        Animator a = new Animator(TestPacks.blob("[]").model(null));
        Mix m = new Mix();
        a.base("idle", 1f, 0.05f);
        a.play(new AnimRef("spin", Blend.REPLACE, 0.1f, 1f), true);
        for (int i = 0; i < 4; i++) {
            a.base("idle", 1f, 0.05f);
            a.tick(0.05f);
        }
        a.fill(m);
        assertEquals(1f, weight(m, "spin"), 1e-4);
        assertEquals(0f, weight(m, "idle"), 1e-4);
        a.play(AnimRef.layer("clap"), false);
        for (int i = 0; i < 4; i++) a.tick(0.05f);
        a.fill(m);
        assertTrue(weight(m, "clap") > 0.9f);
        for (int i = 0; i < 20; i++) a.tick(0.05f);
        a.fill(m);
        assertEquals(0f, weight(m, "clap"), 1e-4, "one-shot layer ended");
        a.stop("spin");
        for (int i = 0; i < 4; i++) a.tick(0.05f);
        a.fill(m);
        assertEquals(1f, weight(m, "idle"), 1e-4);
    }
}
