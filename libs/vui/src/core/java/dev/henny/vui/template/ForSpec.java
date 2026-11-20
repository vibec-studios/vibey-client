package dev.henny.vui.template;

import dev.henny.vui.expr.CompiledExpr;

public record ForSpec(String item, String index, String key, CompiledExpr source) {

    public String[] names() {
        if (index == null && key == null) return new String[]{item};
        if (key == null) return new String[]{item, index};
        return new String[]{item, key, index};
    }
}
