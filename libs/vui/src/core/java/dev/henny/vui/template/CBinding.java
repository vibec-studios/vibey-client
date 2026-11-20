package dev.henny.vui.template;

import dev.henny.vui.expr.CompiledExpr;

public record CBinding(String name, CompiledExpr expr) {
}
