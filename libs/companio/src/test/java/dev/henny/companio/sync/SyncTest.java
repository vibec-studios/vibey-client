package dev.henny.companio.sync;

import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.support.TestPacks;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SyncTest {
    private static Snapshot snap(String name) {
        return new Snapshot("test", "0123456789abcdef", "blob", "default", name, 1.2f, 10.25, 64, -3.5, 90f,
                "minecraft:overworld", "walk", List.of("clap"), List.of("hat"), "Hi ♥", false);
    }

    @Test
    void roundTrip() {
        Snapshot s = snap("Bubbles");
        Snapshot back = SnapshotCodec.decode(SnapshotCodec.encode(s));
        assertEquals(s, back);
        assertTrue(SnapshotCodec.encode(s).length() < 400);
    }

    @Test
    void rejectsAndSanitizesBadInput() {
        assertNull(SnapshotCodec.decode("{"));
        assertNull(SnapshotCodec.decode("{\"v\":2}"));
        assertNull(SnapshotCodec.decode(SnapshotCodec.encode(snap("x")).replace("\"0123456789abcdef\"", "\"zz\"")));
        assertNull(SnapshotCodec.decode(SnapshotCodec.encode(snap("x")).replace("10.25", "1e300")));
        assertNull(SnapshotCodec.decode("x".repeat(2000)));
        Snapshot s = SnapshotCodec.decode(SnapshotCodec.encode(snap("§cRed\u0007Name that is far too long for a tag")));
        assertNotNull(s);
        assertEquals("RedName that is far too", s.name());
    }

    @Test
    void remotePuppetsNeedTheSamePack() {
        CompanionDef def = TestPacks.blob("[]");
        RemoteCompanions remote = new RemoteCompanions((key, hash) -> key.equals("test:blob") && hash.equals("0123456789abcdef") ? def : null);
        UUID p = UUID.randomUUID();
        remote.receive(p, SnapshotCodec.encode(snap("A")), 0);
        assertEquals(1, remote.all().size());
        remote.receive(UUID.randomUUID(), SnapshotCodec.encode(snap("B")).replace("0123456789abcdef", "fedcba9876543210"), 0);
        assertEquals(1, remote.all().size(), "unknown hash is ignored");
        remote.tick(1, 0.05f);
        Puppet puppet = remote.byPlayer().get(p);
        assertEquals("walk", puppet.animator().baseName());
        assertFalse(puppet.hiddenBones(null)[2], "hat prop shown");
        remote.receive(p, SnapshotCodec.GONE, 2);
        assertTrue(remote.all().isEmpty());
        remote.receive(p, SnapshotCodec.encode(snap("A")), 3);
        remote.tick(3 + Puppet.TIMEOUT + 1, 0.05f);
        assertTrue(remote.all().isEmpty(), "stale puppets disappear");
    }
}
