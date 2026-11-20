package dev.henny.vui.expr.node;

import dev.henny.vui.expr.Expr;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.expr.Values;

public record Binary(BinaryOp op, Expr left, Expr right) implements Expr {
    @Override
    public Object eval(Scope scope) {
        Object a = left.eval(scope);
        Object b = right.eval(scope);
        return switch (op) {
            case ADD -> Values.add(a, b);
            case SUB -> Values.toNumber(a) - Values.toNumber(b);
            case MUL -> Values.toNumber(a) * Values.toNumber(b);
            case DIV -> Values.toNumber(a) / Values.toNumber(b);
            case MOD -> Values.toNumber(a) % Values.toNumber(b);
            case EQ -> Values.looseEquals(a, b);
            case NE -> !Values.looseEquals(a, b);
            case LT -> Values.compare(a, b) < 0;
            case LE -> Values.compare(a, b) <= 0;
            case GT -> Values.compare(a, b) > 0;
            case GE -> Values.compare(a, b) >= 0;
        };
    }
}
