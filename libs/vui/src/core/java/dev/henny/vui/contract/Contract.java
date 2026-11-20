package dev.henny.vui.contract;

import dev.henny.vui.diag.SourcePos;

import java.util.List;

public record Contract(List<Entry> fields, List<Entry> actions) {
    public static final Contract EMPTY = new Contract(List.of(), List.of());

    public boolean isEmpty() {
        return fields.isEmpty() && actions.isEmpty();
    }

    public record Entry(String name, String type, boolean optional, SourcePos pos) {
    }
}
