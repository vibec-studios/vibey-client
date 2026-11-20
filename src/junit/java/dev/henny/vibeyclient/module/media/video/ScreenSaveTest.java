package dev.henny.vibeyclient.module.media.video;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.r3.anchor.BlockFaceAnchor;
import dev.henny.r3.math.Face;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.persist.PlacementCodec;
import dev.henny.r3.persist.PlacementRecord;
import dev.henny.r3.interact.SizeLimits;
import dev.henny.vibeyclient.module.media.video.controls.Shapes;
import dev.henny.vibeyclient.module.media.video.play.PixelSize;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenSaveTest {
    static final String OLD = """
            {"screens": {"sp:World": [
              {"id": "6f1c1d3e-2b9c-4a39-9e80-6b9cbd6b1f10", "dim": "minecraft:overworld", "x": 10.5, "y": 66.25, "z": -3.98,
               "normal": "south", "up": "up", "w": 3.0, "h": 1.6875, "kind": "FILE", "uri": "C:/v.mp4", "glow": false,
               "loop": true, "vol": 0.5, "shared": false, "pos": 4200},
              {"id": "0c1f9a0e-0000-4000-8000-000000000001", "w": 3.0, "h": 1.6875}
            ], "mp:other": []}}
            """;

    @Test
    void oldScreensBecomeR3Placements() {
        JsonObject data = JsonParser.parseString(OLD).getAsJsonObject();
        List<PlacementRecord> records = LegacyScreens.take(data, "sp:World");
        assertEquals(1, records.size(), "the overlay-only entry is not placed");
        PlacementRecord r = records.get(0);
        assertEquals("vibeyclient:video", r.kind);
        assertFalse(r.fullBright);
        WorldObject o = PlacementCodec.decode(r, SizeLimits.DEFAULT);
        assertEquals(3.0, o.transform().width(), 1e-6);
        Vector3d normal = o.transform().normal(new Vector3d());
        assertEquals(1.0, normal.z, 1e-5, "faces south");
        BlockFaceAnchor anchor = assertInstanceOf(BlockFaceAnchor.class, o.anchor());
        assertEquals(Face.SOUTH, anchor.face());
        assertEquals(-5, anchor.blockZ(), "the wall block behind the screen");
        assertFalse(data.getAsJsonObject("screens").has("sp:World"), "moved, not copied");
        assertTrue(data.getAsJsonObject("screens").has("mp:other"));
    }

    @Test
    void featureDataRoundTripsAndComesBackPaused() {
        MediaScreen s = new MediaScreen(UUID.randomUUID());
        s.source = MediaSource.parse("https://example.org/a.mp4");
        s.glowing = false;
        s.volume = 0.3f;
        s.setTimeline(true, 12_000);
        MediaScreen back = new MediaScreen(s.id);
        ScreenData.apply(ScreenData.encode(s, false), back, false);
        assertEquals(s.source, back.source);
        assertFalse(back.glowing);
        assertEquals(0.3f, back.volume, 1e-6);
        assertFalse(back.playing(), "saved screens come back paused");
        assertTrue(back.expectedMs() >= 12_000);
    }

    @Test
    void othersNeverSeeLocalSettings() {
        MediaScreen s = new MediaScreen(UUID.randomUUID());
        s.source = MediaSource.parse("https://example.org/a.mp4");
        s.setTimeline(true, 0);
        JsonObject shared = ScreenData.encode(s, true);
        assertFalse(shared.has("vol"));
        assertFalse(shared.has("shared"));
        assertTrue(shared.get("playing").getAsBoolean());
        JsonObject bad = new JsonObject();
        bad.addProperty("kind", "NOPE");
        bad.addProperty("uri", "x");
        assertNull(ScreenData.source(bad));
    }

    @Test
    void shapesAndDecodeSize() {
        assertEquals("21:9", Shapes.label(2.3f));
        assertEquals(1f, Shapes.aspect("1:1"), 1e-6);
        assertArrayEquals(new int[] {1280, 720}, PixelSize.of(720, 16f / 9f, false));
        assertArrayEquals(new int[] {720, 1280}, PixelSize.of(720, 9f / 16f, false));
        int[] web = PixelSize.of(360, 16f / 9f, true);
        assertTrue(web[0] >= 480 && web[1] >= 270);
        assertTrue(new JsonArray().isEmpty());
    }
}
