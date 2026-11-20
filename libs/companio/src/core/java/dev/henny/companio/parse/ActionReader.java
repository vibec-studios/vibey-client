package dev.henny.companio.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.companio.def.Step;
import dev.henny.companio.def.action.Action;
import dev.henny.companio.expr.Expr;
import dev.henny.companio.pack.PackLimits;
import dev.henny.companio.pack.Problems;

import java.util.ArrayList;
import java.util.List;

final class ActionReader {
    static final List<String> KINDS = List.of("anim", "stop_anim", "prop", "sound", "particle", "say", "need", "var",
            "cooldown", "behavior", "stop", "move", "look", "hop", "random");

    private ActionReader() {}

    static List<Step> list(JsonObject o, String key, String at, ReadContext c, boolean timed) {
        JsonArray a = Json.arr(o, key, at, c.problems());
        return a == null ? List.of() : list(a, Json.at(at, key), c, timed, 0);
    }

    static List<Step> list(JsonArray a, String at, ReadContext c, boolean timed, int depth) {
        List<Step> out = new ArrayList<>();
        if (a.size() > PackLimits.MAX_ACTIONS) c.problems().error(at, "too many actions (at most " + PackLimits.MAX_ACTIONS + ")");
        for (int i = 0; i < Math.min(a.size(), PackLimits.MAX_ACTIONS); i++) {
            Step s = step(a.get(i), Json.at(at, i), c, timed, depth);
            if (s != null) out.add(s);
        }
        return List.copyOf(out);
    }

    private static Step step(JsonElement e, String at, ReadContext c, boolean timed, int depth) {
        Problems p = c.problems();
        if (!e.isJsonObject()) {
            p.error(at, "an action must be an object { \"do\": … }");
            return null;
        }
        JsonObject o = e.getAsJsonObject();
        String kind = Json.str(o, "do", at, p, null);
        if (kind == null || !KINDS.contains(kind)) {
            p.error(Json.at(at, "do"), kind == null ? "missing \"do\" (" + String.join(", ", KINDS) + ")"
                    : "unknown action \"" + kind + "\"" + dev.henny.companio.expr.Suggest.hint(kind, KINDS));
            return null;
        }
        Expr cond = ExprReader.read(o, "if", at, c.names(), p, Expr.TRUE);
        float chance = Json.num(o, "chance", at, p, 1f, 0f, 1f);
        float when = Json.num(o, "at", at, p, 0f, 0f, PackLimits.MAX_AT);
        if (when > 0 && !timed) p.warn(Json.at(at, "at"), "\"at\" only works in start and repeat lists");
        Action action = ActionKinds.read(kind, o, at, c, depth);
        if (action == null || cond == null) return null;
        return new Step(action, cond, chance, when);
    }
}
