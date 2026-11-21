package dev.henny.vibeyclient.module.clips.store;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipTimelineTest {
    @Test
    void rewindSaveWritesKeyframesCutsAndHint() {
        ClipTimeline.Builder b = new ClipTimeline.Builder();
        for (long t = 0; t < 300_000; t += 2000) b.keyframe(t);
        b.cut(120_000);
        ClipTimeline.Timeline t = b.build(300_000, 30_000);
        JsonObject o = new JsonObject();
        ClipTimeline.write(o, t);
        assertEquals(150, o.getAsJsonArray("keyframes_ms").size());
        assertEquals(120_000, o.getAsJsonArray("cuts_ms").get(0).getAsLong());
        assertTrue(o.get("rewind").getAsBoolean());
        ClipTimeline.Trim hint = ClipTimeline.readTrim(o, "trim_hint", 300_000);
        assertEquals(270_000, hint.inMs());
        assertEquals(300_000, hint.outMs());
        assertEquals(List.of(0L, 2000L), ClipTimeline.readPoints(o, "keyframes_ms").subList(0, 2));
    }

    @Test
    void hintSnapsToTheKeyframeBefore() {
        ClipTimeline.Builder b = new ClipTimeline.Builder();
        b.keyframe(0);
        b.keyframe(2100);
        b.keyframe(4050);
        ClipTimeline.Timeline t = b.build(6000, 3000);
        assertEquals(2100, t.hint().inMs());
    }

    @Test
    void shortClipHasOnlyKeyframes() {
        ClipTimeline.Builder b = new ClipTimeline.Builder();
        b.keyframe(0);
        b.keyframe(2000);
        b.cut(0);
        JsonObject o = new JsonObject();
        ClipTimeline.write(o, b.build(4000, -1));
        assertTrue(o.has("keyframes_ms"));
        assertFalse(o.has("cuts_ms"));
        assertFalse(o.has("rewind"));
        assertFalse(o.has("trim_hint"));
    }

    @Test
    void badTrimIsIgnored() {
        JsonObject o = new JsonObject();
        JsonObject tr = new JsonObject();
        tr.addProperty("in_ms", 5000);
        tr.addProperty("out_ms", 1000);
        o.add("trim", tr);
        assertNull(ClipTimeline.readTrim(o, "trim", 10_000));
        assertNull(ClipTimeline.readTrim(o, "missing", 10_000));
    }
}
