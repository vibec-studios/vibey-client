package dev.henny.vui.expr.node;

import dev.henny.vui.access.Members;
import dev.henny.vui.expr.EvalException;
import dev.henny.vui.expr.Expr;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.expr.Values;

public record CallMethod(Expr target, String name, Expr[] args) implements Expr {
    @Override
    public Object eval(Scope scope) {
        Object t = target.eval(scope);
        if (t == null || t == Scope.MISSING) throw new EvalException("cannot call " + name + "() on " + Values.typeName(t), null);
        Object[] values = new Object[args.length];
        for (int i = 0; i < args.length; i++) values[i] = args[i].eval(scope);
        Object result = Members.call(t, name, values);
        if (result == Scope.MISSING) {
            throw new EvalException("no method " + name + "(" + args.length + " args) on " + t.getClass().getSimpleName(), null);
        }
        return result;
    }
}
