package dev.henny.cosmetico.parse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.cosmetico.model.Cube;
import dev.henny.cosmetico.model.Face;

import java.util.List;

import static dev.henny.cosmetico.parse.Json.bool;
import static dev.henny.cosmetico.parse.Json.num;
import static dev.henny.cosmetico.parse.Json.str;

final class CubeReader {
    private CubeReader() {}

    static Cube read(String bone, JsonObject c, int defTex, Assets assets, List<String> warnings) {
        String where = "bone " + bone + " cube";
        float[] from = Json.vec(c.get("from"), where + " from", Float.NaN);
        float[] to = Json.vec(c.get("to"), where + " to", Float.NaN);
        if (Float.isNaN(from[0]) || Float.isNaN(to[0])) throw new BundleException(where + ": from/to are required");
        normalise(from, to);
        float inflate = (float) num(c, "inflate", 0);
        boolean mirror = bool(c, "mirror", false);
        float[] rot = c.has("rotation") ? Json.vec(c.get("rotation"), where + " rotation", 0) : null;
        if (rot != null && rot[0] == 0 && rot[1] == 0 && rot[2] == 0) rot = null;
        float[] pivot = rot == null ? null : Json.vec(c.get("pivot"), where + " pivot", 0);
        int tex = texture(str(c, "texture"), defTex, assets, where + ": texture \"");
        if (assets.textures.isEmpty()) throw new BundleException(where + ": bundle has no textures");
        if (!(c.get("uv") instanceof JsonObject uv)) throw new BundleException(where + ": uv is required");
        if (uv.has("box")) {
            float[] box = Json.floats(uv.get("box"), 2, where + " uv.box");
            return new Cube(from, to, inflate, mirror, pivot, rot, box, tex, null);
        }
        Face[] faces = faces(uv, tex, assets, where);
        if (faces == null) {
            warnings.add(where + " has no faces");
            return null;
        }
        return new Cube(from, to, inflate, mirror, pivot, rot, null, tex, faces);
    }

    private static void normalise(float[] from, float[] to) {
        for (int k = 0; k < 3; k++) {
            if (from[k] > to[k]) {
                float t = from[k];
                from[k] = to[k];
                to[k] = t;
            }
        }
    }

    private static int texture(String id, int def, Assets assets, String error) {
        if (id == null) return def;
        Integer i = assets.texture(id);
        if (i == null) throw new BundleException(error + id + "\" does not exist");
        return i;
    }

    private static Face[] faces(JsonObject uv, int tex, Assets assets, String where) {
        Face[] faces = new Face[6];
        boolean any = false;
        for (int f = 0; f < 6; f++) {
            String fn = Limits.FACE_NAMES[f];
            JsonElement fe = uv.get(fn);
            if (fe == null || fe.isJsonNull()) continue;
            float[] r;
            int ft = tex;
            if (fe instanceof JsonObject fo) {
                r = Json.floats(fo.get("uv"), 4, where + " uv." + fn);
                ft = texture(str(fo, "texture"), tex, assets, where + " uv." + fn + ": texture \"");
            } else {
                r = Json.floats(fe, 4, where + " uv." + fn);
            }
            faces[f] = new Face(r[0], r[1], r[2], r[3], ft);
            any = true;
        }
        return any ? faces : null;
    }
}
