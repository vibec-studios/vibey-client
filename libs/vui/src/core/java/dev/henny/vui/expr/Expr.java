package dev.henny.vui.expr;

public interface Expr {
    Object eval(Scope scope);

    default boolean isAssignable() {
        return false;
    }

    default void assign(Scope scope, Object value) {
        throw new EvalException("cannot assign to this expression", "v-model needs a name or a path like a.b");
    }

    default boolean isConstant() {
        return false;
    }
}
