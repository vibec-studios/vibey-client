package dev.henny.vibeyclient.module.media.video;

import com.google.gson.JsonObject;

public final class ScreenData {
    private ScreenData() {}

    public static JsonObject encode(MediaScreen s, boolean forOthers) {
        JsonObject o = new JsonObject();
        if (s.source != null) {
            o.addProperty("kind", s.source.kind().name());
            o.addProperty("uri", s.source.uri());
        }
        o.addProperty("glow", s.glowing);
        o.addProperty("loop", s.loop);
        o.addProperty("playing", s.playing());
        o.addProperty("pos", s.expectedMs());
        if (!forOthers) {
            o.addProperty("vol", s.volume);
            o.addProperty("shared", s.shared);
        }
        return o;
    }

    public static void apply(JsonObject o, MediaScreen s, boolean fromOthers) {
        if (o == null) return;
        s.source = source(o);
        if (o.has("glow")) s.glowing = o.get("glow").getAsBoolean();
        if (o.has("loop")) s.loop = o.get("loop").getAsBoolean();
        if (!fromOthers) {
            if (o.has("vol")) s.volume = Math.clamp(o.get("vol").getAsFloat(), 0f, 1f);
            if (o.has("shared")) s.shared = o.get("shared").getAsBoolean();
        }
        long pos = o.has("pos") ? o.get("pos").getAsLong() : 0;
        boolean playing = fromOthers && o.has("playing") && o.get("playing").getAsBoolean();
        s.setTimeline(playing, pos);
    }

    public static MediaSource source(JsonObject o) {
        if (o == null || !o.has("kind") || !o.has("uri")) return null;
        try {
            String uri = o.get("uri").getAsString();
            if (uri.length() > MediaSource.MAX_URI) return null;
            return new MediaSource(MediaSource.Kind.valueOf(o.get("kind").getAsString()), uri);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static JsonObject state(MediaScreen s) {
        JsonObject o = new JsonObject();
        o.addProperty("playing", s.playing());
        o.addProperty("pos", s.expectedMs());
        return o;
    }
}
