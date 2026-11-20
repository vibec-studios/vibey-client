package dev.henny.vui.expr;

public interface Scope {
    Object MISSING = new Object() {
        @Override
        public String toString() {
            return "undefined";
        }
    };

    Object lookup(String name);

    boolean assign(String name, Object value);

    Object call(String name, Object[] args);

    Scope parent();

    default Object controller() {
        Scope p = parent();
        return p == null ? null : p.controller();
    }
}
