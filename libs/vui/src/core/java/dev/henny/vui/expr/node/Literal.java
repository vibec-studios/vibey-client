package dev.henny.vui.expr.node;

import dev.henny.vui.expr.Expr;
import dev.henny.vui.expr.Scope;

public record Literal(Object value) implements Expr {
    @Override
    public Object eval(Scope scope) {
        return value;
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
