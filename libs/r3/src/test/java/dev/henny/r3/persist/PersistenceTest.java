package dev.henny.r3.persist;

import com.google.gson.JsonObject;
import dev.henny.r3.anchor.BlockFaceAnchor;
import dev.henny.r3.anchor.CameraAnchor;
import dev.henny.r3.anchor.EntityAnchor;
import dev.henny.r3.anchor.FaceSnap;
import dev.henny.r3.anchor.FreeAnchor;
import dev.henny.r3.anchor.ViewFollowAnchor;
import dev.henny.r3.interact.SizeLimits;
import dev.henny.r3.math.Face;
import dev.henny.r3.object.WorldObject;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceTest {
    private static final String WORLD = WorldKeys.server("Play.Example.org ");

    private WorldObject wallScreen() {
        WorldObject o = WorldObject.create("vibey:video").dimension("minecraft:overworld").owner(UUID.randomUUID());
        o.transform().size(3f, 1.6875f).scale(1.5f).position(10.5, 70.25, -3.02);
        FaceSnap.orientation(Face.NORTH, Face.UP, o.transform().orientation());
        o.anchor(new BlockFaceAnchor(Face.NORTH, Face.UP, 10, 70, -3));
        o.locked(true).fullBright(false).revision(7);
        return o;
    }

    @Test
    void roundTripThroughJson() {
        WorldObject original = wallScreen();
        JsonObject data = new JsonObject();
        data.addProperty("uri", "https://example.org/a.mp4");
        PlacementStore store = new PlacementStore();
        store.put(WORLD, PlacementCodec.encode(original, data));
        PlacementStore loaded = PlacementStore.fromJson(store.toJson());
        PlacementRecord r = loaded.get(WORLD, original.id());
        WorldObject copy = PlacementCodec.decode(r, SizeLimits.DEFAULT);
        assertTrue(copy.transform().sameAs(original.transform(), 1e-5));
        assertEquals("minecraft:overworld", copy.dimension());
        assertEquals(original.owner(), copy.owner());
        assertTrue(copy.lockedByUser());
        assertFalse(copy.fullBright());
        assertEquals(7, copy.revision());
        BlockFaceAnchor a = (BlockFaceAnchor) copy.anchor();
        assertEquals(Face.NORTH, a.face());
        assertEquals(-3, a.blockZ());
        assertEquals("https://example.org/a.mp4", r.data.get("uri").getAsString());
        assertEquals("mp:play.example.org", WORLD);
    }

    @Test
    void everyAnchorKindSurvives() {
        UUID player = UUID.randomUUID();
        assertInstanceOf(EntityAnchor.class, roundTrip(EntityAnchor.aboveHead(player, 0.4)));
        EntityAnchor e = (EntityAnchor) roundTrip(EntityAnchor.aboveHead(player, 0.4));
        assertEquals(player, e.entity());
        assertEquals(0.4, e.offset().y(), 1e-9);
        CameraAnchor c = (CameraAnchor) roundTrip(new CameraAnchor(2.5, 0, 6, 4.5f));
        assertEquals(6f, c.pitchOffset(), 1e-6);
        assertEquals(48, ((ViewFollowAnchor) roundTrip(new ViewFollowAnchor(48))).distance(), 1e-9);
        assertSame(FreeAnchor.INSTANCE, roundTrip(FreeAnchor.INSTANCE));
    }

    private dev.henny.r3.anchor.Anchor roundTrip(dev.henny.r3.anchor.Anchor a) {
        return AnchorCodec.decode(AnchorCodec.encode(a));
    }

    @Test
    void brokenAnchorsFallBackToFree() {
        AnchorRecord r = new AnchorRecord();
        r.type = "BLOCK_FACE";
        r.face = "up";
        r.up = "down";
        assertSame(FreeAnchor.INSTANCE, AnchorCodec.decode(r));
        r.type = "WORMHOLE";
        assertSame(FreeAnchor.INSTANCE, AnchorCodec.decode(r));
        r.type = "ENTITY";
        r.entity = "not-a-uuid";
        assertSame(FreeAnchor.INSTANCE, AnchorCodec.decode(r));
    }

    @Test
    void invalidRecordsAreSkippedOnLoad() {
        String json = "{\"format\":1,\"worlds\":{\"sp:w\":["
                + "{\"id\":\"a\",\"kind\":\"k\",\"x\":1,\"y\":2,\"z\":3,\"qw\":1,\"width\":1,\"height\":1,\"scale\":1},"
                + "{\"id\":\"b\",\"kind\":\"k\",\"x\":1e300,\"y\":2,\"z\":3,\"qw\":1},"
                + "{\"id\":\"c\",\"kind\":\"k\",\"qx\":0,\"qy\":0,\"qz\":0,\"qw\":0},"
                + "\"garbage\"]}}";
        PlacementStore store = PlacementStore.fromJson(json);
        assertEquals(1, store.list("sp:w").size());
        assertEquals(0, PlacementStore.fromJson("{not json").worldKeys().size());
    }

    @Test
    void sizesAreClampedToTheLimits() {
        PlacementRecord r = PlacementCodec.encode(wallScreen(), null);
        r.width = 500;
        r.scale = 1;
        WorldObject o = PlacementCodec.decode(r, SizeLimits.DEFAULT);
        assertEquals(16f, o.transform().worldWidth(), 1e-4);
    }

    @Test
    void storeKeepsWorldsAndKindsApart() {
        PlacementStore store = new PlacementStore();
        WorldObject a = wallScreen();
        WorldObject b = WorldObject.create("vibey:lyrics");
        store.put(WORLD, PlacementCodec.encode(a, null));
        store.put(WORLD, PlacementCodec.encode(b, null));
        store.put(WorldKeys.singleplayer("New World"), PlacementCodec.encode(wallScreen(), null));
        assertEquals(1, store.list(WORLD, "vibey:video").size());
        store.replaceAll(WORLD, "vibey:video", java.util.List.of());
        assertEquals(1, store.list(WORLD).size());
        assertTrue(store.remove(WORLD, b.id()));
        assertFalse(store.worldKeys().contains(WORLD));
        assertEquals(1, store.worldKeys().size());
    }

    @Test
    void quaternionIsNormalisedOnLoad() {
        PlacementRecord r = PlacementCodec.encode(wallScreen(), null);
        r.qx *= 3;
        r.qy *= 3;
        r.qz *= 3;
        r.qw *= 3;
        WorldObject o = PlacementCodec.decode(r, SizeLimits.DEFAULT);
        assertEquals(-1, o.transform().normal(new Vector3d()).z, 1e-5);
    }
}
