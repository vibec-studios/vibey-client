package dev.henny.vui.template;

import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.expr.CompiledExpr;

import java.util.List;

public record CText(List<Object> parts, SourcePos pos) implements CNode {

    public boolean isStatic() {
        for (Object p : parts) if (p instanceof CompiledExpr) return false;
        return true;
    }

    public String staticText() {
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) if (p instanceof String s) sb.append(s);
        return sb.toString();
    }
}
