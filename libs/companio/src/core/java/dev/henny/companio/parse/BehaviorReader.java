package dev.henny.companio.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.companio.def.AnimRef;
import dev.henny.companio.def.BehaviorDef;
import dev.henny.companio.def.Blend;
import dev.henny.companio.def.LookMode;
import dev.henny.companio.def.MoveMode;
import dev.henny.companio.def.Range;
import dev.henny.companio.def.RepeatDef;
import dev.henny.companio.expr.Expr;
import dev.henny.companio.expr.Suggest;
import dev.henny.companio.pack.PackLimits;
import dev.henny.companio.pack.Problems;
import dev.henny.companio.sense.Variables;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BehaviorReader {
    private BehaviorReader() {}

    static Set<String> ids(JsonArray a) {
        Set<String> ids = new LinkedHashSet<>();
        if (a == null) return ids;
        for (JsonElement e : a) {
            if (e.isJsonObject() && Json.isString(e.getAsJsonObject().get("id"))) ids.add(e.getAsJsonObject().get("id").getAsString());
        }
        return ids;
    }

    static List<BehaviorDef> read(JsonArray a, ReadContext c) {
        List<BehaviorDef> out = new ArrayList<>();
        if (a == null) return out;
        Set<String> seen = new LinkedHashSet<>();
        if (a.size() > PackLimits.MAX_BEHAVIORS) c.problems().error("behaviors", "too many behaviors (at most " + PackLimits.MAX_BEHAVIORS + ")");
        for (int i = 0; i < Math.min(a.size(), PackLimits.MAX_BEHAVIORS); i++) {
            String at = Json.at("behaviors", i);
            if (!a.get(i).isJsonObject()) {
                c.problems().error(at, "must be an object { \"id\": …, \"priority\": … }");
                continue;
            }
            BehaviorDef b = one(a.get(i).getAsJsonObject(), at, c);
            if (b == null) continue;
            if (!seen.add(b.id())) c.problems().error(Json.at(at, "id"), "behavior \"" + b.id() + "\" exists twice");
            else out.add(b);
        }
        return out;
    }

    private static BehaviorDef one(JsonObject o, String at, ReadContext c) {
        Problems p = c.problems();
        Json.known(o, at, p, "id", "priority", "when", "while", "on", "duration", "minDuration", "cooldown", "weight",
                "interrupt", "obeys", "move", "look", "anim", "baseAnim", "props", "needs", "start", "repeat", "end");
        String id = Json.str(o, "id", at, p, null);
        if (id == null) {
            p.error(Json.at(at, "id"), "needs an id");
            return null;
        }
        if (!AssetReader.id(id, Json.at(at, "id"), p)) return null;
        int mark = p.mark();
        Expr when = ExprReader.read(o, "when", at, c.names(), p, Expr.TRUE);
        Expr whileExpr = ExprReader.read(o, "while", at, c.names(), p, when);
        String on = Json.str(o, "on", at, p, null);
        if (on != null && !Variables.EVENTS.contains(on)) p.error(Json.at(at, "on"), "unknown event \"" + on + "\"" + Suggest.hint(on, Variables.EVENTS));
        Range duration = Json.range(o, "duration", at, p, null, 0.05f, 3600f);
        MoveMode move = mode(o, at, p);
        LookMode look = look(o, at, p);
        AnimRef anim = AnimRefReader.read(o.get("anim"), Json.at(at, "anim"), c, Blend.ADD);
        String baseAnim = Json.str(o, "baseAnim", at, p, null);
        if (baseAnim != null && !c.hasAnimation(baseAnim)) p.error(Json.at(at, "baseAnim"), "the model has no animation \"" + baseAnim + "\" (" + c.animations() + ")");
        BehaviorDef b = new BehaviorDef(id, (int) Json.num(o, "priority", at, p, 10, 0, 1000), when, whileExpr, on, duration,
                Json.num(o, "minDuration", at, p, 1f, 0f, 600f), Json.num(o, "cooldown", at, p, 0f, 0f, 86400f),
                Json.num(o, "weight", at, p, 1f, 0.01f, 100f), Json.bool(o, "interrupt", at, p, true),
                Json.bool(o, "obeys", at, p, true), move, look, anim, baseAnim, props(o, at, c), needs(o, at, c),
                ActionReader.list(o, "start", at, c, true), repeats(o, at, c), ActionReader.list(o, "end", at, c, false));
        return p.newErrorsSince(mark) ? null : b;
    }

    private static MoveMode mode(JsonObject o, String at, Problems p) {
        String m = Json.str(o, "move", at, p, null);
        if (m == null) return null;
        MoveMode mode = MoveMode.of(m);
        if (mode == null) p.error(Json.at(at, "move"), "unknown movement mode \"" + m + "\"" + Suggest.hint(m, modeNames()));
        return mode;
    }

    private static List<String> modeNames() {
        List<String> l = new ArrayList<>();
        for (MoveMode m : MoveMode.values()) l.add(m.id());
        return l;
    }

    private static LookMode look(JsonObject o, String at, Problems p) {
        String m = Json.str(o, "look", at, p, null);
        if (m == null) return null;
        LookMode mode = LookMode.of(m);
        if (mode == null) p.error(Json.at(at, "look"), "must be player, move, sky, around, target or none");
        return mode;
    }

    private static Map<String, Boolean> props(JsonObject o, String at, ReadContext c) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        JsonObject po = Json.obj(o, "props", at, c.problems());
        if (po == null) return out;
        for (Map.Entry<String, JsonElement> e : po.entrySet()) {
            String path = Json.at(Json.at(at, "props"), e.getKey());
            if (!c.props().containsKey(e.getKey())) c.problems().error(path, "no prop \"" + e.getKey() + "\"" + Suggest.hint(e.getKey(), c.props().keySet()));
            else if (!e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isBoolean()) c.problems().error(path, "must be true or false");
            else out.put(e.getKey(), e.getValue().getAsBoolean());
        }
        return out;
    }

    private static Map<String, Float> needs(JsonObject o, String at, ReadContext c) {
        Map<String, Float> out = new LinkedHashMap<>();
        JsonObject no = Json.obj(o, "needs", at, c.problems());
        if (no == null) return out;
        for (Map.Entry<String, JsonElement> e : no.entrySet()) {
            String path = Json.at(Json.at(at, "needs"), e.getKey());
            if (!c.needs().contains(e.getKey())) c.problems().error(path, "no need \"" + e.getKey() + "\"" + Suggest.hint(e.getKey(), c.needs()));
            else out.put(e.getKey(), Json.num(no, e.getKey(), Json.at(at, "needs"), c.problems(), 0f, -10f, 10f));
        }
        return out;
    }

    private static List<RepeatDef> repeats(JsonObject o, String at, ReadContext c) {
        JsonArray a = Json.arr(o, "repeat", at, c.problems());
        List<RepeatDef> out = new ArrayList<>();
        if (a == null) return out;
        if (a.size() > PackLimits.MAX_REPEATS) c.problems().error(Json.at(at, "repeat"), "at most " + PackLimits.MAX_REPEATS + " entries");
        for (int i = 0; i < Math.min(a.size(), PackLimits.MAX_REPEATS); i++) {
            String path = Json.at(Json.at(at, "repeat"), i);
            if (!a.get(i).isJsonObject()) {
                c.problems().error(path, "must be { \"every\": [3, 6], \"actions\": [ … ] }");
                continue;
            }
            JsonObject r = a.get(i).getAsJsonObject();
            Json.known(r, path, c.problems(), "every", "actions");
            Range every = Json.range(r, "every", path, c.problems(), null, 0.05f, 3600f);
            if (every == null) c.problems().error(Json.at(path, "every"), "needs seconds, a number or [min, max]");
            else out.add(new RepeatDef(every, ActionReader.list(r, "actions", path, c, true)));
        }
        return out;
    }
}
