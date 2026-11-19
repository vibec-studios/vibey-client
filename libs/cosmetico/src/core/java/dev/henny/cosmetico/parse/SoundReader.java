package dev.henny.cosmetico.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.cosmetico.model.Sound;

import static dev.henny.cosmetico.parse.Json.str;

final class SoundReader {
    private SoundReader() {}

    static void readAll(JsonObject o, Assets a) {
        if (!(o.get("sounds") instanceof JsonArray sa)) return;
        for (JsonElement e : sa) {
            if (!(e instanceof JsonObject s)) continue;
            String sid = str(s, "id");
            if (sid == null) throw new BundleException("sounds[].id is required");
            byte[] ogg = Json.base64(str(s, "ogg"), "sound " + sid);
            if (ogg.length > Limits.MAX_SOUND) throw new BundleException("sound " + sid + " is " + ogg.length + " bytes (max " + Limits.MAX_SOUND + ")");
            if (ogg.length < 4 || ogg[0] != 'O' || ogg[1] != 'g' || ogg[2] != 'g' || ogg[3] != 'S') throw new BundleException("sound " + sid + " is not OGG");
            if (a.soundIndex.put(sid, a.sounds.size()) != null) throw new BundleException("duplicate sound id " + sid);
            a.sounds.add(new Sound(sid, ogg));
        }
    }
}
