package dev.henny.companio.parse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.companio.expr.Expr;
import dev.henny.companio.expr.ExprException;
import dev.henny.companio.expr.Names;
import dev.henny.companio.pack.Problems;

final class ExprReader {
    private ExprReader() {}

    static Expr read(JsonObject o, String key, String path, Names names, Problems p, Expr def) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return def;
        if (Json.isNumber(e)) return Expr.constant(e.getAsDouble());
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) return e.getAsBoolean() ? Expr.TRUE : Expr.FALSE;
        if (!Json.isString(e)) {
            p.error(Json.at(path, key), "must be an expression in quotes, e.g. \"weather.rain && time.night\"");
            return def;
        }
        return compile(e.getAsString(), Json.at(path, key), names, p);
    }

    static Expr compile(String src, String path, Names names, Problems p) {
        try {
            return Expr.compile(src, names);
        } catch (ExprException ex) {
            String where = ex.position() > 0 ? " (at character " + (ex.position() + 1) + ")" : "";
            p.error(path, ex.getMessage() + where);
            return null;
        }
    }
}
