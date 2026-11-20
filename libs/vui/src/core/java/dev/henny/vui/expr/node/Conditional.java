package dev.henny.vui.expr.node;

import dev.henny.vui.expr.Expr;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.expr.Values;

public record Conditional(Expr test, Expr then, Expr otherwise) implements Expr {
    @Override
    public Object eval(Scope scope) {
        return Values.truthy(test.eval(scope)) ? then.eval(scope) : otherwise.eval(scope);
    }
}
