package dev.henny.vui.expr.node;

import dev.henny.vui.expr.EvalException;
import dev.henny.vui.expr.Expr;
import dev.henny.vui.expr.Scope;

public record Identifier(String name) implements Expr {
    @Override
    public Object eval(Scope scope) {
        return scope.lookup(name);
    }

    @Override
    public boolean isAssignable() {
        return true;
    }

    @Override
    public void assign(Scope scope, Object value) {
        if (!scope.assign(name, value)) {
            throw new EvalException("cannot write '" + name + "'",
                    "add a setter, a non-final field or a State<?> named '" + name + "' to the controller");
        }
    }
}
