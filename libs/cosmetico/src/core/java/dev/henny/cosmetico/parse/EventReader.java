package dev.henny.cosmetico.parse;

import com.google.gson.JsonObject;
import dev.henny.cosmetico.model.Bone;
import dev.henny.cosmetico.model.Event;
import dev.henny.cosmetico.model.EventType;
import dev.henny.cosmetico.model.Locator;
import dev.henny.cosmetico.model.Model;

import java.util.List;

import static dev.henny.cosmetico.parse.Json.bool;
import static dev.henny.cosmetico.parse.Json.num;
import static dev.henny.cosmetico.parse.Json.str;

final class EventReader {
    private EventReader() {}

    static Event read(String anim, JsonObject e, Model model, Assets assets, List<String> warnings) {
        String where = "animation " + anim + " event";
        float t = (float) num(e, "t", 0);
        String type = str(e, "type");
        Locator loc = locator(e, model, where);
        if ("particle".equals(type)) return particle(e, t, loc, assets, where);
        if ("sound".equals(type)) return sound(e, t, loc, assets, where);
        if ("visibility".equals(type)) {
            String bone = str(e, "bone");
            Bone b = model == null || bone == null ? null : model.bone(bone);
            if (b == null) throw new BundleException(where + ": bone \"" + bone + "\" does not exist");
            return new Event(t, EventType.VISIBILITY, null, -1, -1, null, 0, null, null, 0, 0, 0, false, 0, 0, b.index, bool(e, "visible", true));
        }
        warnings.add(where + ": unknown type " + type);
        return null;
    }

    private static Locator locator(JsonObject e, Model model, String where) {
        String ln = str(e, "locator");
        if (ln == null) return null;
        Locator loc = model == null ? null : model.locators.get(ln);
        if (loc == null) throw new BundleException(where + ": locator \"" + ln + "\" does not exist");
        return loc;
    }

    private static Event particle(JsonObject e, float t, Locator loc, Assets assets, String where) {
        String sprite = str(e, "sprite");
        String particle = str(e, "particle");
        int si = -1;
        if (sprite != null) {
            Integer i = assets.spriteIndex.get(sprite);
            if (i == null) throw new BundleException(where + ": sprite \"" + sprite + "\" does not exist");
            si = i;
            particle = null;
        } else if (particle == null) {
            throw new BundleException(where + ": particle or sprite is required");
        } else if (!particle.contains(":")) {
            particle = "minecraft:" + particle;
        }
        int count = Math.max(1, Math.min(64, (int) num(e, "count", 1)));
        return new Event(t, EventType.PARTICLE, particle, si, -1, loc, count,
                Json.vec(e.get("spread"), where + " spread", 0), Json.vec(e.get("velocity"), where + " velocity", 0),
                (float) num(e, "lifetime", 1), (float) num(e, "size", 0.15), (float) num(e, "gravity", 0),
                bool(e, "emissive", false), 1, 1, -1, true);
    }

    private static Event sound(JsonObject e, float t, Locator loc, Assets assets, String where) {
        String sound = str(e, "sound");
        if (sound == null) throw new BundleException(where + ": sound is required");
        Integer si = assets.soundIndex.get(sound);
        if (si == null && !sound.contains(":")) throw new BundleException(where + ": embedded sound \"" + sound + "\" does not exist");
        return new Event(t, EventType.SOUND, sound, -1, si == null ? -1 : si, loc, 1, null, null, 0, 0, 0, false,
                (float) num(e, "volume", 1), (float) num(e, "pitch", 1), -1, true);
    }
}
