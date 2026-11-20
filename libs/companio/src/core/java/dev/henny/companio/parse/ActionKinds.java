package dev.henny.companio.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.companio.def.Blend;
import dev.henny.companio.def.LookMode;
import dev.henny.companio.def.MoveMode;
import dev.henny.companio.def.ParticleDef;
import dev.henny.companio.def.SoundDef;
import dev.henny.companio.def.Step;
import dev.henny.companio.def.action.*;
import dev.henny.companio.expr.Expr;
import dev.henny.companio.expr.Suggest;
import dev.henny.companio.pack.Problems;
import dev.henny.companio.text.TextRef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ActionKinds {
    private static final Set<String> COMMON = Set.of("do", "if", "chance", "at");

    private ActionKinds() {}

    static Action read(String kind, JsonObject o, String at, ReadContext c, int depth) {
        Problems p = c.problems();
        return switch (kind) {
            case "anim" -> {
                known(o, at, p, "name", "blend", "fade", "speed");
                var ref = AnimRefReader.fields(o, at, c, Blend.ADD);
                yield ref == null ? null : new AnimAction(ref);
            }
            case "stop_anim" -> {
                known(o, at, p, "name");
                String n = name(o, at, p);
                yield n == null ? null : new StopAnimAction(n);
            }
            case "prop" -> prop(o, at, c);
            case "sound" -> sound(o, at, c);
            case "particle" -> particle(o, at, c);
            case "say" -> {
                known(o, at, p, "text", "seconds");
                TextRef t = TextReader.read(o, "text", at, c.lang(), p, null);
                if (t == null) p.error(Json.at(at, "text"), "needs a text");
                yield t == null ? null : new SayAction(t, Json.num(o, "seconds", at, p, 2.5f, 0.5f, 10f));
            }
            case "need" -> need(o, at, c);
            case "var" -> var(o, at, c);
            case "cooldown" -> {
                known(o, at, p, "name", "seconds");
                String n = name(o, at, p);
                yield n == null ? null : new CooldownAction(n, Json.num(o, "seconds", at, p, 10f, 0f, 3600f));
            }
            case "behavior" -> {
                known(o, at, p, "id");
                String id = Json.str(o, "id", at, p, null);
                if (id == null || !c.behaviors().contains(id)) {
                    p.error(Json.at(at, "id"), id == null ? "needs a behavior id" : "no behavior \"" + id + "\"" + Suggest.hint(id, c.behaviors()));
                    yield null;
                }
                yield new BehaviorAction(id);
            }
            case "stop" -> {
                known(o, at, p);
                yield new StopAction();
            }
            case "move" -> {
                known(o, at, p, "mode");
                MoveMode m = MoveMode.of(Json.str(o, "mode", at, p, ""));
                if (m == null) p.error(Json.at(at, "mode"), "unknown movement mode");
                yield m == null ? null : new MoveAction(m);
            }
            case "look" -> {
                known(o, at, p, "at");
                LookMode m = LookMode.of(Json.str(o, "at", at, p, ""));
                if (m == null) p.error(Json.at(at, "at"), "must be player, move, sky, around or none");
                yield m == null ? null : new LookAction(m);
            }
            case "hop" -> {
                known(o, at, p, "height");
                yield new HopAction(Json.num(o, "height", at, p, 0.5f, 0.1f, 2f));
            }
            case "random" -> random(o, at, c, depth);
            default -> null;
        };
    }

    private static void known(JsonObject o, String at, Problems p, String... keys) {
        List<String> all = new ArrayList<>(COMMON);
        all.addAll(List.of(keys));
        Json.known(o, at, p, all.toArray(new String[0]));
    }

    private static String name(JsonObject o, String at, Problems p) {
        String n = Json.str(o, "name", at, p, null);
        if (n == null) p.error(Json.at(at, "name"), "needs a name");
        return n;
    }

    private static Action prop(JsonObject o, String at, ReadContext c) {
        known(o, at, c.problems(), "name", "show");
        String n = name(o, at, c.problems());
        if (n == null) return null;
        if (!c.props().containsKey(n)) {
            c.problems().error(Json.at(at, "name"), "no prop \"" + n + "\"" + Suggest.hint(n, c.props().keySet()));
            return null;
        }
        return new PropAction(n, Json.bool(o, "show", at, c.problems(), true));
    }

    private static Action sound(JsonObject o, String at, ReadContext c) {
        Problems p = c.problems();
        known(o, at, p, "name", "volume", "pitch");
        String n = name(o, at, p);
        if (n == null) return null;
        SoundDef base = c.sounds().get(n);
        if (base == null && !n.matches(AssetReader.RESOURCE_ID)) {
            p.error(Json.at(at, "name"), "no sound \"" + n + "\"" + Suggest.hint(n, c.sounds().keySet()) + " (or use a sound id like \"minecraft:entity.cat.purr\")");
            return null;
        }
        if (base == null) base = new SoundDef(n, null, n, 1f, 1f, 1f);
        var pitch = Json.range(o, "pitch", at, p, new dev.henny.companio.def.Range(base.pitchMin(), base.pitchMax()), 0.5f, 2f);
        return new SoundAction(new SoundDef(base.id(), base.file(), base.event(), Json.num(o, "volume", at, p, base.volume(), 0f, 2f), pitch.min(), pitch.max()));
    }

    private static Action particle(JsonObject o, String at, ReadContext c) {
        String n = Json.str(o, "name", at, c.problems(), null);
        if (n == null) {
            c.problems().error(Json.at(at, "name"), "needs a particle burst name or a particle id");
            return null;
        }
        ParticleDef base = c.particles().get(n);
        if (base == null && !n.matches(AssetReader.RESOURCE_ID)) {
            c.problems().error(Json.at(at, "name"), "no particle burst \"" + n + "\"" + Suggest.hint(n, c.particles().keySet()));
            return null;
        }
        if (base == null) base = new ParticleDef(n, n, 1, null, ParticleDef.ZERO, ParticleDef.ZERO);
        ParticleDef d = AssetReader.particle(base.id(), o, at, c, base);
        return d == null ? null : new ParticleAction(d);
    }

    private static Action need(JsonObject o, String at, ReadContext c) {
        Map<String, Float> deltas = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            if (COMMON.contains(e.getKey())) continue;
            if (!c.needs().contains(e.getKey())) {
                c.problems().error(Json.at(at, e.getKey()), "no need \"" + e.getKey() + "\"" + Suggest.hint(e.getKey(), c.needs()));
                continue;
            }
            deltas.put(e.getKey(), Json.num(o, e.getKey(), at, c.problems(), 0f, -1f, 1f));
        }
        if (deltas.isEmpty()) c.problems().error(at, "needs at least one need, e.g. \"energy\": -0.1");
        return deltas.isEmpty() ? null : new NeedAction(Map.copyOf(deltas));
    }

    private static Action var(JsonObject o, String at, ReadContext c) {
        known(o, at, c.problems(), "name", "value");
        String n = name(o, at, c.problems());
        if (n == null) return null;
        if (!c.vars().contains(n)) {
            c.problems().error(Json.at(at, "name"), "no var \"" + n + "\" (declare it in \"vars\")" + Suggest.hint(n, c.vars()));
            return null;
        }
        Expr v = ExprReader.read(o, "value", at, c.names(), c.problems(), null);
        if (v == null && !o.has("value")) c.problems().error(Json.at(at, "value"), "needs a value");
        return v == null ? null : new VarAction(n, v);
    }

    private static Action random(JsonObject o, String at, ReadContext c, int depth) {
        known(o, at, c.problems(), "of");
        JsonArray of = Json.arr(o, "of", at, c.problems());
        if (of == null || of.isEmpty()) {
            c.problems().error(Json.at(at, "of"), "needs a list of action lists");
            return null;
        }
        if (depth >= 3) {
            c.problems().error(at, "random inside random inside random is too deep");
            return null;
        }
        List<List<Step>> options = new ArrayList<>();
        for (int i = 0; i < of.size(); i++) {
            String path = Json.at(Json.at(at, "of"), i);
            JsonElement e = of.get(i);
            if (e.isJsonArray()) options.add(ActionReader.list(e.getAsJsonArray(), path, c, true, depth + 1));
            else if (e.isJsonObject()) {
                JsonArray one = new JsonArray();
                one.add(e);
                options.add(ActionReader.list(one, path, c, true, depth + 1));
            } else c.problems().error(path, "must be a list of actions");
        }
        return new RandomAction(List.copyOf(options));
    }
}
