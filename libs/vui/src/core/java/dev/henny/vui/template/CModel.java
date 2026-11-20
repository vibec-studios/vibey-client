package dev.henny.vui.template;

import dev.henny.vui.expr.CompiledExpr;

import java.util.List;

public record CModel(String prop, List<String> modifiers, CompiledExpr target) {

    public boolean trim() {
        return modifiers.contains("trim");
    }

    public boolean number() {
        return modifiers.contains("number");
    }

    public boolean lazy() {
        return modifiers.contains("lazy");
    }
}
