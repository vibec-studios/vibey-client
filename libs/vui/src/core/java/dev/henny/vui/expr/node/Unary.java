package dev.henny.vui.expr.node;

import dev.henny.vui.expr.Expr;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.expr.Values;

public record Unary(char op, Expr operand) implements Expr {
    @Override
    public Object eval(Scope scope) {
        Object v = operand.eval(scope);
        return switch (op) {
            case '!' -> !Values.truthy(v);
            case '-' -> -Values.toNumber(v);
            default -> Values.toNumber(v);
        };
    }
}
