package dev.henny.cosmetico.dev;

import dev.henny.cosmetico.fixtures.Fixtures;
import dev.henny.cosmetico.fixtures.TestEngine;
import dev.henny.cosmetico.fixtures.TestPng;
import dev.henny.cosmetico.registry.ItemSource;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevItemServiceTest {
    private final TestEngine t = new TestEngine();

    @Test
    void pushEquipsCosmeticsAndPlaysEmotes() {
        PushResult r = t.engine.devItems.push(Fixtures.cosmetic("t:hat", "hat"), PushAction.EQUIP, null);
        assertTrue(r.ok());
        assertEquals(ItemSource.DEV, t.engine.items.get("t:hat").source());
        t.engine.devItems.push(Fixtures.emote("t:e").toString().getBytes(StandardCharsets.UTF_8), PushAction.EQUIP, null);
        t.engine.devItems.push(Fixtures.cosmetic("t:bag", "back"), PushAction.NONE, null);
        assertEquals(List.of("equip t:hat", "play t:e"), t.actions.calls);
        assertEquals(List.of("t:hat", "t:bag", "t:e"), t.engine.devItems.ids());
    }

    @Test
    void invalidBundlesAreReportedNotRegistered() {
        var o = Fixtures.cosmetic("t:hat", "tail");
        PushResult r = t.engine.devItems.push(o, PushAction.EQUIP, null);
        assertFalse(r.ok());
        assertEquals("t:hat", r.id());
        assertTrue(r.error().contains("tail"));
        assertNull(t.engine.items.get("t:hat"));
    }

    @Test
    void thumbnailsAreCheckedAndKeptOnRePush() {
        PushResult bad = t.engine.devItems.push(Fixtures.cosmetic("t:hat", "hat"), PushAction.NONE, new byte[5]);
        assertEquals(List.of("thumbnail ignored: not a PNG"), bad.warnings());
        assertNull(t.engine.items.get("t:hat").thumbnail());
        t.engine.devItems.push(Fixtures.cosmetic("t:hat", "hat"), PushAction.NONE, TestPng.of(32, 32));
        String key = t.engine.items.get("t:hat").thumbnail();
        assertTrue(key.startsWith("dev:t:hat#"));
        t.engine.devItems.push(Fixtures.cosmetic("t:hat", "hat"), PushAction.NONE, null);
        assertEquals(key, t.engine.items.get("t:hat").thumbnail());
    }

    @Test
    void removeStopsThePlayingDevEmote() {
        t.engine.devItems.push(Fixtures.emote("t:e"), PushAction.PLAY, null);
        assertTrue(t.engine.devItems.remove("t:e"));
        assertFalse(t.engine.devItems.remove("t:e"));
        assertEquals(List.of("play t:e", "stop"), t.actions.calls);
    }
}
