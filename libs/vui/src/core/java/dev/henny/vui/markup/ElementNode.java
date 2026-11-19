package dev.henny.vui.markup;

import dev.henny.vui.diag.SourcePos;

import java.util.List;

public record ElementNode(String tag, List<Attribute> attributes, List<TemplateNode> children, SourcePos pos)
        implements TemplateNode {

    public Attribute find(AttributeKind kind) {
        for (Attribute a : attributes) if (a.kind() == kind) return a;
        return null;
    }

    public Attribute find(AttributeKind kind, String name) {
        for (Attribute a : attributes) if (a.kind() == kind && a.name().equals(name)) return a;
        return null;
    }

    public String staticValue(String name) {
        Attribute a = find(AttributeKind.STATIC, name);
        return a == null ? null : a.value();
    }

    public boolean has(AttributeKind kind) {
        return find(kind) != null;
    }
}
