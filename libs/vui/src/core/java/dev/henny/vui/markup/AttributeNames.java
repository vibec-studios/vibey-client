package dev.henny.vui.markup;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourcePos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AttributeNames {
    private AttributeNames() {
    }

    public static Attribute classify(String raw, String value, SourcePos pos, SourcePos valuePos, Diagnostics diagnostics) {
        if (raw.startsWith(":")) return withModifiers(AttributeKind.BIND, raw.substring(1), value, pos, valuePos);
        if (raw.startsWith("v-bind:")) return withModifiers(AttributeKind.BIND, raw.substring(7), value, pos, valuePos);
        if (raw.startsWith("@")) return withModifiers(AttributeKind.EVENT, raw.substring(1), value, pos, valuePos);
        if (raw.startsWith("v-on:")) return withModifiers(AttributeKind.EVENT, raw.substring(5), value, pos, valuePos);
        if (raw.startsWith("#")) return withModifiers(AttributeKind.SLOT, raw.substring(1), value, pos, valuePos);
        if (raw.equals("v-slot")) return simple(AttributeKind.SLOT, "default", value, pos, valuePos);
        if (raw.startsWith("v-slot:")) return withModifiers(AttributeKind.SLOT, raw.substring(7), value, pos, valuePos);
        if (raw.equals("v-model")) return simple(AttributeKind.MODEL, "", value, pos, valuePos);
        if (raw.startsWith("v-model")) return modelWithArgument(raw, value, pos, valuePos);
        if (raw.startsWith("v-")) return directive(raw, value, pos, valuePos, diagnostics);
        if (raw.equals("ref")) return simple(AttributeKind.REF, "ref", value, pos, valuePos);
        if (raw.equals("key")) return simple(AttributeKind.KEY, "key", value, pos, valuePos);
        return simple(AttributeKind.STATIC, raw, value, pos, valuePos);
    }

    private static Attribute modelWithArgument(String raw, String value, SourcePos pos, SourcePos valuePos) {
        String rest = raw.substring(7);
        if (rest.startsWith(":")) rest = rest.substring(1);
        return withModifiers(AttributeKind.MODEL, rest, value, pos, valuePos);
    }

    private static Attribute directive(String raw, String value, SourcePos pos, SourcePos valuePos, Diagnostics diagnostics) {
        switch (raw) {
            case "v-if": return simple(AttributeKind.IF, "v-if", value, pos, valuePos);
            case "v-else-if": return simple(AttributeKind.ELSE_IF, "v-else-if", value, pos, valuePos);
            case "v-else": return simple(AttributeKind.ELSE, "v-else", value, pos, valuePos);
            case "v-for": return simple(AttributeKind.FOR, "v-for", value, pos, valuePos);
            case "v-show": return simple(AttributeKind.SHOW, "v-show", value, pos, valuePos);
            case "v-text": return simple(AttributeKind.HTML, "text", value, pos, valuePos);
            default:
                diagnostics.error(pos, "unknown directive '" + raw + "'",
                        "known: v-if, v-else-if, v-else, v-for, v-show, v-model, v-slot, v-bind, v-on, v-text");
                return simple(AttributeKind.STATIC, raw, value, pos, valuePos);
        }
    }

    private static Attribute withModifiers(AttributeKind kind, String nameAndModifiers, String value, SourcePos pos,
                                           SourcePos valuePos) {
        String[] parts = nameAndModifiers.split("\\.");
        String name = parts.length == 0 ? "" : parts[0];
        List<String> modifiers = parts.length > 1
                ? new ArrayList<>(Arrays.asList(parts).subList(1, parts.length)) : List.of();
        return new Attribute(kind, name, modifiers, value, pos, valuePos);
    }

    private static Attribute simple(AttributeKind kind, String name, String value, SourcePos pos, SourcePos valuePos) {
        return new Attribute(kind, name, List.of(), value, pos, valuePos);
    }
}
