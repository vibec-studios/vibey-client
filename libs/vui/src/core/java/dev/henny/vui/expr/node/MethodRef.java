package dev.henny.vui.expr.node;

import dev.henny.vui.expr.EvalException;
import dev.henny.vui.expr.Expr;
import dev.henny.vui.expr.Scope;

public record MethodRef(String name) implements Expr {
    public static final String EVENT = "$event";

    @Override
    public Object eval(Scope scope) {
        Object event = scope.lookup(EVENT);
        Object result = event == Scope.MISSING ? Scope.MISSING : scope.call(name, new Object[]{event});
        if (result == Scope.MISSING) result = scope.call(name, new Object[0]);
        if (result == Scope.MISSING) {
            throw new EvalException("no method '" + name + "' on the controller", "add void " + name + "() or " + name + "(Object event)");
        }
        return result;
    }
}
