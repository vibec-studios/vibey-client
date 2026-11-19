package dev.henny.vui.expr.node;

import dev.henny.vui.expr.Expr;
import dev.henny.vui.expr.Scope;

public record Assign(Expr target, Expr value) implements Expr {
    @Override
    public Object eval(Scope scope) {
        Object v = value.eval(scope);
        target.assign(scope, v);
        return v;
    }
}
