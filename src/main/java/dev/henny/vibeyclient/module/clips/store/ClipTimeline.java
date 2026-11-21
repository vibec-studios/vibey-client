package dev.henny.vibeyclient.module.clips.store;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class ClipTimeline {
    public static final int MAX_POINTS = 2000;

    private ClipTimeline() {}

    public record Trim(long inMs, long outMs) {}

    public record Timeline(List<Long> keyframes, List<Long> cuts, boolean rewind, Trim hint) {}

    public static final class Builder {
        private final List<Long> keyframes = new ArrayList<>();
        private final List<Long> cuts = new ArrayList<>();

        public void keyframe(long ms) {
            if (keyframes.size() >= MAX_POINTS) return;
            if (!keyframes.isEmpty() && keyframes.get(keyframes.size() - 1) >= ms) return;
            keyframes.add(Math.max(0, ms));
        }

        public void cut(long ms) {
            if (ms <= 0 || cuts.size() >= MAX_POINTS) return;
            if (!cuts.isEmpty() && cuts.get(cuts.size() - 1) >= ms) return;
            cuts.add(ms);
        }

        public Timeline build(long durationMs, long trimHintMs) {
            List<Long> k = new ArrayList<>();
            for (long t : keyframes) if (t < durationMs) k.add(t);
            if (k.isEmpty() || k.get(0) != 0) k.add(0, 0L);
            List<Long> c = new ArrayList<>();
            for (long t : cuts) if (t < durationMs) c.add(t);
            Trim hint = null;
            if (trimHintMs > 0) {
                long in = snapDown(k, Math.max(0, durationMs - trimHintMs));
                hint = new Trim(in, durationMs);
            }
            return new Timeline(List.copyOf(k), List.copyOf(c), trimHintMs > 0, hint);
        }
    }

    public static long snapDown(List<Long> keyframes, long ms) {
        long best = 0;
        for (long k : keyframes) {
            if (k > ms) break;
            best = k;
        }
        return best;
    }

    public static void write(JsonObject o, Timeline t) {
        if (t == null) return;
        o.add("keyframes_ms", array(t.keyframes()));
        if (!t.cuts().isEmpty()) o.add("cuts_ms", array(t.cuts()));
        if (t.rewind()) o.addProperty("rewind", true);
        if (t.hint() != null) o.add("trim_hint", trim(t.hint()));
    }

    public static JsonObject trim(Trim t) {
        JsonObject j = new JsonObject();
        j.addProperty("in_ms", t.inMs());
        j.addProperty("out_ms", t.outMs());
        return j;
    }

    public static Trim readTrim(JsonObject o, String field, long durationMs) {
        JsonElement e = o.get(field);
        if (e == null || !e.isJsonObject()) return null;
        JsonObject j = e.getAsJsonObject();
        try {
            long in = j.get("in_ms").getAsLong();
            long out = j.get("out_ms").getAsLong();
            if (in < 0 || out <= in || out > durationMs + 1000) return null;
            return new Trim(in, Math.min(out, durationMs));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static List<Long> readPoints(JsonObject o, String field) {
        List<Long> out = new ArrayList<>();
        JsonElement e = o.get(field);
        if (e == null || !e.isJsonArray()) return out;
        for (JsonElement x : e.getAsJsonArray()) {
            try {
                out.add(x.getAsLong());
            } catch (RuntimeException ignored) {
            }
            if (out.size() >= MAX_POINTS) break;
        }
        return out;
    }

    private static JsonArray array(List<Long> v) {
        JsonArray a = new JsonArray();
        for (long x : v) a.add(x);
        return a;
    }
}
