package dev.henny.cosmetico.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.cosmetico.model.Attach;
import dev.henny.cosmetico.model.Bone;
import dev.henny.cosmetico.model.Cube;
import dev.henny.cosmetico.model.Kind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static dev.henny.cosmetico.parse.Json.bool;
import static dev.henny.cosmetico.parse.Json.str;

final class BoneReader {
    private BoneReader() {}

    static int cubeCount(JsonObject b) {
        return b.get("cubes") instanceof JsonArray ca ? ca.size() : 0;
    }

    static Bone read(String n, int i, JsonObject b, Kind kind, Map<String, Integer> index, Bone[] done, int defTex,
                     Assets assets, List<String> warnings) {
        String parentName = str(b, "parent");
        int parent = parentName == null ? -1 : index.get(parentName);
        Attach attach = parent < 0 ? attach(n, b, kind, warnings) : null;
        float[] pivot = Json.vec(b.get("pivot"), "bone " + n + " pivot", 0);
        float[] rot = Json.vec(b.get("rotation"), "bone " + n + " rotation", 0);
        boolean emissive = bool(b, "emissive", false), translucent = bool(b, "translucent", false);
        boolean effE = emissive || (parent >= 0 && done[parent].effectiveEmissive);
        boolean effT = translucent || (parent >= 0 && done[parent].effectiveTranslucent);
        List<Cube> cl = new ArrayList<>();
        if (b.get("cubes") instanceof JsonArray ca) {
            for (JsonElement ce : ca) {
                if (!(ce instanceof JsonObject c)) throw new BundleException("bone " + n + ": cubes must be objects");
                Cube cube = CubeReader.read(n, c, defTex, assets, warnings);
                if (cube != null) cl.add(cube);
            }
        }
        return new Bone(n, i, parent, attach, pivot, rot, cl.toArray(new Cube[0]), emissive, translucent, effE, effT);
    }

    private static Attach attach(String n, JsonObject b, Kind kind, List<String> warnings) {
        String at = str(b, "attach");
        Attach attach = Attach.of(at);
        if (attach != null) return attach;
        attach = kind == Kind.EMOTE ? Attach.ROOT : Attach.BODY;
        warnings.add("bone \"" + n + "\" " + (at == null ? "has no attach" : "has unknown attach \"" + at + "\"")
                + ", using " + attach.name().toLowerCase(Locale.ROOT));
        return attach;
    }
}
