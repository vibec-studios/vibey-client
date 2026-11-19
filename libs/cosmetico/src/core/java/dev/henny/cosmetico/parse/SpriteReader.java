package dev.henny.cosmetico.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.cosmetico.model.Sprite;

import static dev.henny.cosmetico.parse.Json.str;

final class SpriteReader {
    private SpriteReader() {}

    static void readAll(JsonObject o, Assets a) {
        if (!(o.get("sprites") instanceof JsonArray sa)) return;
        for (JsonElement e : sa) {
            if (!(e instanceof JsonObject s)) continue;
            String sid = str(s, "id");
            Integer ti = a.texture(str(s, "texture"));
            if (sid == null) throw new BundleException("sprites[].id is required");
            if (ti == null) throw new BundleException("sprite " + sid + ": texture \"" + str(s, "texture") + "\" does not exist");
            float[] uv = Json.floats(s.get("uv"), 4, "sprite " + sid + " uv");
            if (a.spriteIndex.put(sid, a.sprites.size()) != null) throw new BundleException("duplicate sprite id " + sid);
            a.sprites.add(new Sprite(sid, ti, uv[0], uv[1], uv[2], uv[3]));
        }
    }
}
