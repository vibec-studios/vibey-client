package dev.henny.companio.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.companio.def.VariantDef;
import dev.henny.companio.pack.PackFiles;
import dev.henny.companio.pack.PackLimits;
import dev.henny.companio.pack.PackPaths;
import dev.henny.companio.pack.Problems;
import dev.henny.cosmetico.model.Attach;
import dev.henny.cosmetico.model.Bone;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.parse.BundleException;
import dev.henny.cosmetico.parse.Bundles;
import dev.henny.cosmetico.version.VersionGate;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ModelReader {
    private ModelReader() {}

    static Map<String, Bundle> read(PackFiles files, String modelPath, List<VariantDef> variants, Map<String, byte[]> sounds,
                                    VersionGate gate, Problems p, String companionFile) {
        Map<String, Bundle> out = new LinkedHashMap<>();
        p.file(modelPath);
        JsonObject base = JsonFiles.object(files, modelPath, p);
        if (base == null) {
            p.file(companionFile);
            return out;
        }
        addSounds(base, sounds);
        Bundle first = parse(base, gate, p, "");
        if (first != null) checkRoots(first, p);
        for (VariantDef v : variants) {
            if (v.textures().isEmpty()) {
                if (first != null) out.put(v.id(), first);
                continue;
            }
            JsonObject copy = base.deepCopy();
            p.file(companionFile);
            if (!patch(copy, v, files, p)) continue;
            p.file(modelPath);
            Bundle b = parse(copy, gate, p, "variant " + v.id() + ": ");
            if (b != null) out.put(v.id(), b);
        }
        if (variants.isEmpty() && first != null) out.put("default", first);
        p.file(companionFile);
        return out;
    }

    private static Bundle parse(JsonObject o, VersionGate gate, Problems p, String prefix) {
        try {
            Bundle b = Bundles.parse(o, gate);
            if (prefix.isEmpty()) for (String w : b.warnings) p.warn("", w);
            if (b.model == null) p.error("", prefix + "the bundle has no model");
            return b.model == null ? null : b;
        } catch (BundleException ex) {
            p.error("", prefix + ex.getMessage());
            return null;
        } catch (RuntimeException ex) {
            p.error("", prefix + "cannot read the bundle: " + ex);
            return null;
        }
    }

    private static void checkRoots(Bundle b, Problems p) {
        for (Bone bone : b.model.bones) {
            if (bone.parent < 0 && bone.attach != Attach.WORLD) p.warn("", "root bone \"" + bone.name + "\" is not \"attach\": \"world\" and is not drawn");
        }
    }

    private static void addSounds(JsonObject o, Map<String, byte[]> sounds) {
        if (sounds.isEmpty()) return;
        JsonArray list = o.has("sounds") && o.get("sounds").isJsonArray() ? o.getAsJsonArray("sounds") : new JsonArray();
        for (Map.Entry<String, byte[]> e : sounds.entrySet()) {
            String id = PackPaths.fileName(e.getKey());
            boolean exists = false;
            for (JsonElement s : list) if (s.isJsonObject() && id.equals(s.getAsJsonObject().get("id").getAsString())) exists = true;
            if (exists) continue;
            JsonObject s = new JsonObject();
            s.addProperty("id", id);
            s.addProperty("ogg", Base64.getEncoder().encodeToString(e.getValue()));
            list.add(s);
        }
        o.add("sounds", list);
    }

    private static boolean patch(JsonObject o, VariantDef v, PackFiles files, Problems p) {
        JsonArray textures = o.has("textures") && o.get("textures").isJsonArray() ? o.getAsJsonArray("textures") : new JsonArray();
        boolean ok = true;
        for (Map.Entry<String, String> e : v.textures().entrySet()) {
            String path = "variants." + v.id() + ".textures." + e.getKey();
            JsonObject tex = find(textures, e.getKey());
            if (tex == null) {
                p.error(path, "the model has no texture \"" + e.getKey() + "\"");
                ok = false;
                continue;
            }
            byte[] png = readPng(files, e.getValue(), path, p);
            if (png == null || !sameAspect(tex, png, path, p)) {
                ok = false;
                continue;
            }
            tex.addProperty("png", Base64.getEncoder().encodeToString(png));
        }
        return ok;
    }

    private static JsonObject find(JsonArray textures, String id) {
        for (JsonElement t : textures) {
            if (t.isJsonObject() && t.getAsJsonObject().has("id") && id.equals(t.getAsJsonObject().get("id").getAsString())) return t.getAsJsonObject();
        }
        return null;
    }

    private static byte[] readPng(PackFiles files, String file, String path, Problems p) {
        if (!files.exists(file)) {
            p.error(path, "file \"" + file + "\" is missing");
            return null;
        }
        try {
            byte[] b = files.read(file);
            int[] s = Png.size(b);
            if (s == null) p.error(path, "\"" + file + "\" is not a PNG");
            else if (s[0] > PackLimits.MAX_TEXTURE || s[1] > PackLimits.MAX_TEXTURE) p.error(path, "\"" + file + "\" is " + s[0] + "×" + s[1] + ", at most " + PackLimits.MAX_TEXTURE + "×" + PackLimits.MAX_TEXTURE);
            else return b;
        } catch (java.io.IOException ex) {
            p.error(path, "cannot read \"" + file + "\": " + ex.getMessage());
        }
        return null;
    }

    private static boolean sameAspect(JsonObject tex, byte[] png, String path, Problems p) {
        int[] s = Png.size(png);
        int w = tex.has("width") ? tex.get("width").getAsInt() : s[0];
        int h = tex.has("height") ? tex.get("height").getAsInt() : s[1];
        if ((long) s[0] * h != (long) s[1] * w) {
            p.error(path, "PNG is " + s[0] + "×" + s[1] + " but the texture is " + w + "×" + h + " (same aspect needed)");
            return false;
        }
        return true;
    }
}
