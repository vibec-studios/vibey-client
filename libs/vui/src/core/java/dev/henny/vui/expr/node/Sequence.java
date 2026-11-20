package dev.henny.vui.expr.node;

import dev.henny.vui.expr.Expr;
import dev.henny.vui.expr.Scope;

public record Sequence(Expr[] items) implements Expr {
    @Override
    public Object eval(Scope scope) {
        Object last = null;
        for (Expr e : items) last = e.eval(scope);
        return last;
    }
}
