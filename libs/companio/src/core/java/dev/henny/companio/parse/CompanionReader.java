package dev.henny.companio.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.henny.companio.def.BehaviorDef;
import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.def.LookDef;
import dev.henny.companio.def.NeedDef;
import dev.henny.companio.def.ParticleDef;
import dev.henny.companio.def.PropDef;
import dev.henny.companio.def.SoundDef;
import dev.henny.companio.def.TrickDef;
import dev.henny.companio.def.VariantDef;
import dev.henny.companio.pack.PackFiles;
import dev.henny.companio.pack.PackLimits;
import dev.henny.companio.pack.Problems;
import dev.henny.companio.text.Lang;
import dev.henny.companio.text.TextRef;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.version.VersionGate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CompanionReader {
    private CompanionReader() {}

    static CompanionDef read(PackFiles files, JsonObject o, String file, String packId, Lang lang, Map<String, byte[]> soundFiles,
                             VersionGate gate, Problems p) {
        p.file(file);
        int mark = p.mark();
        Json.known(o, "", p, "id", "name", "description", "model", "scale", "size", "box", "variants", "animations", "look",
                "movement", "props", "sounds", "particles", "needs", "vars", "tricks", "behaviors");
        String id = Json.str(o, "id", "", p, null);
        if (id == null) p.error("id", "needs an id");
        else AssetReader.id(id, "id", p);
        TextRef name = TextReader.read(o, "name", "", lang, p, null);
        if (name == null) p.error("name", "needs a name");
        Map<String, NeedDef> needs = StateReader.needs(o, p);
        Map<String, Float> vars = StateReader.vars(o, p);
        List<VariantDef> variants = LooksReader.variants(o, lang, p);
        String modelPath = Json.str(o, "model", "", p, null);
        Map<String, Bundle> models = Map.of();
        if (modelPath == null) p.error("model", "needs a model (a .vibey file in the pack)");
        else models = ModelReader.read(files, modelPath, variants, soundFiles, gate, p, file);
        Bundle model = models.isEmpty() ? null : models.values().iterator().next();
        if (modelPath != null && model == null) return null;
        Map<String, PropDef> props = AssetReader.props(o, model, p);
        Map<String, SoundDef> sounds = AssetReader.sounds(o, soundFiles.keySet(), p);
        JsonArray behaviorsJson = Json.arr(o, "behaviors", "", p);
        Set<String> behaviorIds = BehaviorReader.ids(behaviorsJson);
        ReadContext c = new ReadContext(p, lang, new CompanionNames(needs.keySet(), vars.keySet()), model, props, sounds,
                Map.of(), needs.keySet(), vars.keySet(), behaviorIds);
        Map<String, ParticleDef> particles = AssetReader.particles(o, c);
        c = new ReadContext(p, lang, c.names(), model, props, sounds, particles, needs.keySet(), vars.keySet(), behaviorIds);
        List<BehaviorDef> behaviors = BehaviorReader.read(behaviorsJson, c);
        LookDef look = MovementReader.look(o, model, p);
        CompanionDef def = new CompanionDef(packId, id, name, TextReader.read(o, "description", "", lang, p, TextRef.EMPTY), lang,
                models, variants, Json.num(o, "scale", "", p, 1f, 0.05f, 8f), LooksReader.size(o, p), LooksReader.box(o, p),
                LooksReader.loco(o, model, p), look, MovementReader.boneIndex(model, look.bone()), MovementReader.read(o, p),
                props, sounds, soundFiles, particles, needs, vars, tricks(o, lang, behaviors, p), behaviors);
        return p.newErrorsSince(mark) ? null : def;
    }

    private static List<TrickDef> tricks(JsonObject o, Lang lang, List<BehaviorDef> behaviors, Problems p) {
        List<TrickDef> out = new ArrayList<>();
        JsonArray a = Json.arr(o, "tricks", "", p);
        if (a == null) return out;
        for (int i = 0; i < a.size(); i++) {
            String at = Json.at("tricks", i);
            if (!a.get(i).isJsonObject()) {
                p.error(at, "must be { \"id\": …, \"name\": …, \"behavior\": … }");
                continue;
            }
            if (out.size() == PackLimits.MAX_TRICKS) {
                p.error(at, "too many tricks (at most " + PackLimits.MAX_TRICKS + ")");
                break;
            }
            JsonObject t = a.get(i).getAsJsonObject();
            Json.known(t, at, p, "id", "name", "icon", "behavior");
            String id = Json.str(t, "id", at, p, null);
            String behavior = Json.str(t, "behavior", at, p, null);
            if (id == null || !AssetReader.id(id, Json.at(at, "id"), p)) continue;
            if (behavior == null || behaviors.stream().noneMatch(b -> b.id().equals(behavior))) {
                p.error(Json.at(at, "behavior"), behavior == null ? "needs a behavior id" : "no behavior \"" + behavior + "\"");
                continue;
            }
            out.add(new TrickDef(id, TextReader.read(t, "name", at, lang, p, TextRef.literal(id)), Json.str(t, "icon", at, p, "★"), behavior));
        }
        return out;
    }
}
