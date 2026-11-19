package dev.henny.vui.markup;

import dev.henny.vui.diag.SourcePos;

import java.util.List;

public record TextNode(List<TextPart> parts, SourcePos pos) implements TemplateNode {

    public boolean isStatic() {
        for (TextPart p : parts) if (p.expression()) return false;
        return true;
    }

    public String staticText() {
        StringBuilder sb = new StringBuilder();
        for (TextPart p : parts) if (!p.expression()) sb.append(p.text());
        return sb.toString();
    }
}
