package dev.henny.cosmetico.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.cosmetico.model.Sound;
import dev.henny.cosmetico.model.Sprite;
import dev.henny.cosmetico.model.Texture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Assets {
    final List<Texture> textures = new ArrayList<>();
    final Map<String, Integer> textureIndex = new HashMap<>();
    final List<Sound> sounds = new ArrayList<>();
    final Map<String, Integer> soundIndex = new HashMap<>();
    final List<Sprite> sprites = new ArrayList<>();
    final Map<String, Integer> spriteIndex = new HashMap<>();

    static Assets read(JsonObject o, List<String> warnings) {
        Assets a = new Assets();
        if (o.get("textures") instanceof JsonArray ta) {
            for (JsonElement e : ta) {
                if (!(e instanceof JsonObject t)) throw new BundleException("textures entries must be objects");
                Texture tex = TextureReader.read(t, warnings);
                if (a.textureIndex.put(tex.id, a.textures.size()) != null) throw new BundleException("duplicate texture id " + tex.id);
                a.textures.add(tex);
            }
        }
        SoundReader.readAll(o, a);
        SpriteReader.readAll(o, a);
        return a;
    }

    Integer texture(String id) {
        return textureIndex.get(id);
    }
}
