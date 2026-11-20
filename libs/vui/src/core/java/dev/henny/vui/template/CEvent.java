package dev.henny.vui.template;

import dev.henny.vui.expr.CompiledExpr;

import java.util.List;

public record CEvent(String name, List<String> modifiers, CompiledExpr handler) {
}
