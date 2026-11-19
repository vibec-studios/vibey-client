package dev.henny.cosmetico.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.cosmetico.model.Bone;
import dev.henny.cosmetico.model.Kind;
import dev.henny.cosmetico.model.Locator;
import dev.henny.cosmetico.model.Model;
import dev.henny.cosmetico.model.PlayerBone;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.henny.cosmetico.parse.Json.str;

final class ModelReader {
    private ModelReader() {}

    static Model read(JsonObject o, Kind kind, Assets assets, List<String> warnings) {
        if (o.get("model") instanceof JsonObject m) return model(m, kind, assets, warnings);
        if (o.has("model") && !o.get("model").isJsonNull()) throw new BundleException("model must be an object");
        return null;
    }

    private static Model model(JsonObject m, Kind kind, Assets assets, List<String> warnings) {
        int defTex = 0;
        String mt = str(m, "texture");
        if (mt != null) {
            Integer i = assets.texture(mt);
            if (i == null) throw new BundleException("model.texture \"" + mt + "\" does not exist");
            defTex = i;
        }
        Map<String, JsonObject> byName = byName(m, kind);
        List<String> order = BoneOrder.of(byName);
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < order.size(); i++) index.put(order.get(i), i);
        Bone[] bones = new Bone[order.size()];
        int cubes = 0;
        for (int i = 0; i < bones.length; i++) {
            JsonObject b = byName.get(order.get(i));
            cubes += BoneReader.cubeCount(b);
            bones[i] = BoneReader.read(order.get(i), i, b, kind, index, bones, defTex, assets, warnings);
        }
        if (cubes > Limits.MAX_CUBES) throw new BundleException(cubes + " cubes (max " + Limits.MAX_CUBES + ")");
        return new Model(defTex, bones, locators(m, index), cubes);
    }

    private static Map<String, JsonObject> byName(JsonObject m, Kind kind) {
        JsonArray list = m.get("bones") instanceof JsonArray a ? a : new JsonArray();
        if (list.size() > Limits.MAX_BONES) throw new BundleException(list.size() + " bones (max " + Limits.MAX_BONES + ")");
        Map<String, JsonObject> byName = new LinkedHashMap<>();
        for (JsonElement e : list) {
            if (!(e instanceof JsonObject b)) throw new BundleException("bones must be objects");
            String n = str(b, "name");
            if (n == null || n.isEmpty()) throw new BundleException("bone name is required");
            if (byName.put(n, b) != null) throw new BundleException("duplicate bone \"" + n + "\"");
            if (kind == Kind.EMOTE && PlayerBone.of(n) != null) throw new BundleException("bone \"" + n + "\" clashes with a player bone name");
        }
        return byName;
    }

    private static Map<String, Locator> locators(JsonObject m, Map<String, Integer> index) {
        Map<String, Locator> locators = new LinkedHashMap<>();
        if (!(m.get("locators") instanceof JsonObject lo)) return Collections.unmodifiableMap(locators);
        for (Map.Entry<String, JsonElement> e : lo.entrySet()) {
            if (!(e.getValue() instanceof JsonObject l)) throw new BundleException("locator " + e.getKey() + " must be an object");
            String bone = str(l, "bone");
            Integer bi = bone == null ? null : index.get(bone);
            PlayerBone pb = bi == null ? PlayerBone.of(bone) : null;
            if (bi == null && pb == null) throw new BundleException("locator \"" + e.getKey() + "\": bone \"" + bone + "\" does not exist");
            locators.put(e.getKey(), new Locator(e.getKey(), bi == null ? -1 : bi, pb, Json.vec(l.get("position"), "locator " + e.getKey(), 0)));
        }
        return Collections.unmodifiableMap(locators);
    }
}
