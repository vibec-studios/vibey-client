package dev.henny.vui.expr.node;

import dev.henny.vui.expr.Expr;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.expr.Values;

public record Logical(boolean and, Expr left, Expr right) implements Expr {
    @Override
    public Object eval(Scope scope) {
        Object a = left.eval(scope);
        if (and) return Values.truthy(a) ? right.eval(scope) : a;
        return Values.truthy(a) ? a : right.eval(scope);
    }
}
