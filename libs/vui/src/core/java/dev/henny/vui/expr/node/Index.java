package dev.henny.vui.expr.node;

import dev.henny.vui.access.Members;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.expr.EvalException;
import dev.henny.vui.expr.Expr;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.expr.Values;

import java.util.List;

public record Index(Expr target, Expr index) implements Expr {
    @Override
    public Object eval(Scope scope) {
        Object t = target.eval(scope);
        Object i = index.eval(scope);
        if (t instanceof List<?> list) {
            int n = (int) Values.toNumber(i);
            return n >= 0 && n < list.size() ? Values.unwrap(list.get(n)) : Scope.MISSING;
        }
        if (t instanceof Object[] array) {
            int n = (int) Values.toNumber(i);
            return n >= 0 && n < array.length ? array[n] : Scope.MISSING;
        }
        if (t instanceof CharSequence s) {
            int n = (int) Values.toNumber(i);
            return n >= 0 && n < s.length() ? String.valueOf(s.charAt(n)) : Scope.MISSING;
        }
        return Members.get(t, Values.display(i));
    }

    @Override
    public boolean isAssignable() {
        return true;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void assign(Scope scope, Object value) {
        Object t = target.eval(scope);
        Object i = index.eval(scope);
        if (t instanceof ListState list) list.set((int) Values.toNumber(i), value);
        else if (t instanceof List list) list.set((int) Values.toNumber(i), value);
        else if (!Members.set(t, Values.display(i), value)) throw new EvalException("cannot write index " + Values.display(i), null);
    }
}
