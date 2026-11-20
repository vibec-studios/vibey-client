package dev.henny.vui.expr.node;

import dev.henny.vui.access.Members;
import dev.henny.vui.expr.EvalException;
import dev.henny.vui.expr.Expr;
import dev.henny.vui.expr.Scope;

public record Member(Expr target, String name, boolean optional) implements Expr {
    @Override
    public Object eval(Scope scope) {
        Object t = target.eval(scope);
        if (optional && (t == null || t == Scope.MISSING)) return null;
        return Members.get(t, name);
    }

    @Override
    public boolean isAssignable() {
        return true;
    }

    @Override
    public void assign(Scope scope, Object value) {
        Object t = target.eval(scope);
        if (!Members.set(t, name, value)) {
            throw new EvalException("cannot write '" + name + "'", "add a setter, a non-final field or a State<?> named '" + name + "'");
        }
    }
}
