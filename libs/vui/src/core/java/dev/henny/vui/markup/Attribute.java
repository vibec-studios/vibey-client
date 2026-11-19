package dev.henny.vui.markup;

import dev.henny.vui.diag.SourcePos;

import java.util.List;

public record Attribute(AttributeKind kind, String name, List<String> modifiers, String value, SourcePos pos,
                        SourcePos valuePos) {

    public boolean hasModifier(String modifier) {
        return modifiers.contains(modifier);
    }

    public String rawName() {
        return switch (kind) {
            case BIND -> ":" + name;
            case EVENT -> "@" + name;
            case MODEL -> name.isEmpty() ? "v-model" : "v-model:" + name;
            case SLOT -> "#" + name;
            case IF -> "v-if";
            case ELSE_IF -> "v-else-if";
            case ELSE -> "v-else";
            case FOR -> "v-for";
            case SHOW -> "v-show";
            default -> name;
        };
    }
}
