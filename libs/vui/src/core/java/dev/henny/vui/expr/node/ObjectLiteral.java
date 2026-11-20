package dev.henny.vui.expr.node;

import dev.henny.vui.expr.Expr;
import dev.henny.vui.expr.Scope;

import java.util.LinkedHashMap;
import java.util.Map;

public record ObjectLiteral(String[] keys, Expr[] values) implements Expr {
    @Override
    public Object eval(Scope scope) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < keys.length; i++) out.put(keys[i], values[i].eval(scope));
        return out;
    }
}
