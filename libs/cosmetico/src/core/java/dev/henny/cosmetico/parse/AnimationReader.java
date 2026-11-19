package dev.henny.cosmetico.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.cosmetico.model.Animation;
import dev.henny.cosmetico.model.Bone;
import dev.henny.cosmetico.model.Channel;
import dev.henny.cosmetico.model.Event;
import dev.henny.cosmetico.model.Kind;
import dev.henny.cosmetico.model.Model;
import dev.henny.cosmetico.model.PlayerBone;
import dev.henny.cosmetico.model.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.henny.cosmetico.parse.Json.bool;
import static dev.henny.cosmetico.parse.Json.num;

final class AnimationReader {
    private AnimationReader() {}

    static Map<String, Animation> readAll(JsonObject o, Kind kind, Model model, Assets assets, List<String> warnings) {
        Map<String, Animation> animations = new LinkedHashMap<>();
        if (o.get("animations") instanceof JsonObject as) {
            for (Map.Entry<String, JsonElement> e : as.entrySet()) {
                if (!(e.getValue() instanceof JsonObject a)) throw new BundleException("animation " + e.getKey() + " must be an object");
                animations.put(e.getKey(), read(e.getKey(), a, kind, model, assets, warnings));
            }
        }
        return Collections.unmodifiableMap(animations);
    }

    private static Animation read(String name, JsonObject a, Kind kind, Model model, Assets assets, List<String> warnings) {
        float length = (float) num(a, "length", 0);
        if (!(length > 0)) throw new BundleException("animation " + name + ": length must be > 0");
        boolean loop = bool(a, "loop", false);
        boolean hold = bool(a, "hold", false) && !loop;
        List<Track> tracks = tracks(name, a, kind, model, warnings);
        List<Event> events = new ArrayList<>();
        if (a.get("events") instanceof JsonArray ea) {
            for (JsonElement ee : ea) {
                if (!(ee instanceof JsonObject e)) continue;
                Event ev = EventReader.read(name, e, model, assets, warnings);
                if (ev != null) events.add(ev);
            }
        }
        events.sort(Comparator.comparingDouble(e -> e.t));
        return new Animation(name, length, loop, hold, tracks.toArray(new Track[0]), events.toArray(new Event[0]));
    }

    private static List<Track> tracks(String name, JsonObject a, Kind kind, Model model, List<String> warnings) {
        List<Track> tracks = new ArrayList<>();
        if (!(a.get("bones") instanceof JsonObject bo)) return tracks;
        for (Map.Entry<String, JsonElement> e : bo.entrySet()) {
            String bn = e.getKey();
            Bone mb = model == null ? null : model.bone(bn);
            PlayerBone pb = mb == null && kind == Kind.EMOTE ? PlayerBone.of(bn) : null;
            if (mb == null && pb == null) {
                warnings.add("animation " + name + ": bone \"" + bn + "\" does not exist");
                continue;
            }
            if (!(e.getValue() instanceof JsonObject ch)) continue;
            String where = "animation " + name + " bone " + bn;
            Channel r = ChannelReader.read(ch.get("rotation"), where + " rotation", warnings);
            Channel p = ChannelReader.read(ch.get("position"), where + " position", warnings);
            Channel s = ChannelReader.read(ch.get("scale"), where + " scale", warnings);
            if (r == null && p == null && s == null) continue;
            tracks.add(new Track(mb == null ? -1 : mb.index, pb, r, p, s));
        }
        return tracks;
    }
}
