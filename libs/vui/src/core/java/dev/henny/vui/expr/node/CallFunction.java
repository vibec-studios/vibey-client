package dev.henny.vui.expr.node;

import dev.henny.vui.expr.EvalException;
import dev.henny.vui.expr.Expr;
import dev.henny.vui.expr.Scope;

public record CallFunction(String name, Expr[] args) implements Expr {
    @Override
    public Object eval(Scope scope) {
        Object[] values = new Object[args.length];
        for (int i = 0; i < args.length; i++) values[i] = args[i].eval(scope);
        Object result = scope.call(name, values);
        if (result == Scope.MISSING) {
            throw new EvalException("no method " + name + "(" + args.length + " args) on the controller",
                    "add a method '" + name + "' with " + args.length + " parameter(s), or a Model action");
        }
        return result;
    }
}
