package dev.henny.companio.parse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.companio.def.ParticleDef;
import dev.henny.companio.def.PropDef;
import dev.henny.companio.def.Range;
import dev.henny.companio.def.SoundDef;
import dev.henny.companio.pack.PackLimits;
import dev.henny.companio.pack.Problems;
import dev.henny.cosmetico.model.Bone;
import dev.henny.cosmetico.model.Bundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AssetReader {
    static final String RESOURCE_ID = "[a-z0-9_.-]+:[a-z0-9_./-]+";

    private AssetReader() {}

    static Map<String, PropDef> props(JsonObject root, Bundle model, Problems p) {
        Map<String, PropDef> out = new LinkedHashMap<>();
        JsonObject o = Json.obj(root, "props", "", p);
        if (o == null) return out;
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            String at = "props." + e.getKey();
            if (!id(e.getKey(), at, p) || !e.getValue().isJsonObject()) {
                if (!e.getValue().isJsonObject()) p.error(at, "must be an object { \"bones\": [ … ] }");
                continue;
            }
            if (out.size() == PackLimits.MAX_PROPS) {
                p.error(at, "too many props (at most " + PackLimits.MAX_PROPS + ")");
                break;
            }
            JsonObject po = e.getValue().getAsJsonObject();
            Json.known(po, at, p, "bones", "shown");
            List<String> bones = new ArrayList<>();
            List<Integer> idx = new ArrayList<>();
            var arr = Json.arr(po, "bones", at, p);
            if (arr == null || arr.isEmpty()) p.error(Json.at(at, "bones"), "needs at least one bone");
            else for (int i = 0; i < arr.size(); i++) bone(arr.get(i), Json.at(Json.at(at, "bones"), i), model, p, bones, idx);
            out.put(e.getKey(), new PropDef(e.getKey(), List.copyOf(bones), idx.stream().mapToInt(Integer::intValue).toArray(),
                    Json.bool(po, "shown", at, p, false)));
        }
        return out;
    }

    private static void bone(JsonElement e, String at, Bundle model, Problems p, List<String> bones, List<Integer> idx) {
        if (!Json.isString(e)) {
            p.error(at, "must be a bone name");
            return;
        }
        String name = e.getAsString();
        if (model == null) return;
        Bone b = model.model.bone(name);
        if (b == null) {
            p.error(at, "the model has no bone \"" + name + "\"" + BoneNames.hint(model, name));
            return;
        }
        bones.add(name);
        idx.add(b.index);
    }

    static Map<String, SoundDef> sounds(JsonObject root, Set<String> files, Problems p) {
        Map<String, SoundDef> out = new LinkedHashMap<>();
        JsonObject o = Json.obj(root, "sounds", "", p);
        if (o == null) return out;
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            String at = "sounds." + e.getKey();
            if (!id(e.getKey(), at, p)) continue;
            if (!e.getValue().isJsonObject()) {
                p.error(at, "must be an object { \"file\": … } or { \"sound\": … }");
                continue;
            }
            if (out.size() == PackLimits.MAX_SOUNDS) {
                p.error(at, "too many sounds (at most " + PackLimits.MAX_SOUNDS + ")");
                break;
            }
            SoundDef s = sound(e.getKey(), e.getValue().getAsJsonObject(), at, files, p);
            if (s != null) out.put(e.getKey(), s);
        }
        return out;
    }

    static SoundDef sound(String id, JsonObject so, String at, Set<String> files, Problems p) {
        Json.known(so, at, p, "file", "sound", "volume", "pitch", "do", "name", "if", "chance", "at");
        String file = Json.str(so, "file", at, p, null);
        String event = Json.str(so, "sound", at, p, null);
        if ((file == null) == (event == null)) {
            p.error(at, "needs exactly one of \"file\" (an .ogg in the pack) or \"sound\" (a sound id)");
            return null;
        }
        if (file != null && !files.contains(file)) {
            p.error(Json.at(at, "file"), "file \"" + file + "\" is missing or not an OGG Vorbis ≤ 512 KiB");
            return null;
        }
        if (event != null && !event.matches(RESOURCE_ID)) {
            p.error(Json.at(at, "sound"), "\"" + event + "\" is not a sound id like \"minecraft:entity.cat.purr\"");
            return null;
        }
        Range pitch = Json.range(so, "pitch", at, p, Range.of(1f), 0.5f, 2f);
        return new SoundDef(id, file, event, Json.num(so, "volume", at, p, 1f, 0f, 2f), pitch.min(), pitch.max());
    }

    static Map<String, ParticleDef> particles(JsonObject root, ReadContext c) {
        Map<String, ParticleDef> out = new LinkedHashMap<>();
        Problems p = c.problems();
        JsonObject o = Json.obj(root, "particles", "", p);
        if (o == null) return out;
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            String at = "particles." + e.getKey();
            if (!id(e.getKey(), at, p)) continue;
            if (!e.getValue().isJsonObject()) {
                p.error(at, "must be an object { \"particle\": … }");
                continue;
            }
            ParticleDef d = particle(e.getKey(), e.getValue().getAsJsonObject(), at, c, null);
            if (d != null) out.put(e.getKey(), d);
        }
        return out;
    }

    static ParticleDef particle(String id, JsonObject po, String at, ReadContext c, ParticleDef base) {
        Problems p = c.problems();
        Json.known(po, at, p, "particle", "count", "locator", "spread", "velocity", "do", "name", "if", "chance", "at");
        String particle = Json.str(po, "particle", at, p, base == null ? null : base.particle());
        if (particle == null || !particle.matches(RESOURCE_ID)) {
            p.error(Json.at(at, "particle"), particle == null ? "needs a particle id like \"minecraft:heart\"" : "\"" + particle + "\" is not a particle id like \"minecraft:heart\"");
            return null;
        }
        String loc = Json.str(po, "locator", at, p, base == null ? null : base.locator());
        if (loc != null && !c.hasLocator(loc)) {
            p.error(Json.at(at, "locator"), "the model has no locator \"" + loc + "\"");
            loc = null;
        }
        int count = (int) Json.num(po, "count", at, p, base == null ? 1 : base.count(), 1, PackLimits.MAX_PARTICLE_COUNT);
        return new ParticleDef(id, particle, count, loc,
                Json.vec3(po, "spread", at, p, base == null ? ParticleDef.ZERO : base.spread(), 8f),
                Json.vec3(po, "velocity", at, p, base == null ? ParticleDef.ZERO : base.velocity(), 2f));
    }

    static boolean id(String id, String at, Problems p) {
        if (id.matches(PackLimits.ID)) return true;
        p.error(at, "name \"" + id + "\" must be lowercase a-z, 0-9, _ (at most 32)");
        return false;
    }
}
