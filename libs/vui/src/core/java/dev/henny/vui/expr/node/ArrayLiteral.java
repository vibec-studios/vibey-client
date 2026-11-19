package dev.henny.vui.expr.node;

import dev.henny.vui.expr.Expr;
import dev.henny.vui.expr.Scope;

import java.util.ArrayList;
import java.util.List;

public record ArrayLiteral(Expr[] items) implements Expr {
    @Override
    public Object eval(Scope scope) {
        List<Object> out = new ArrayList<>(items.length);
        for (Expr e : items) out.add(e.eval(scope));
        return out;
    }
}
