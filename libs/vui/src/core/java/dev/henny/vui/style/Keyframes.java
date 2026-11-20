package dev.henny.vui.style;

import dev.henny.vui.diag.SourcePos;

import java.util.List;

public record Keyframes(String name, List<Stop> stops, SourcePos pos) {

    public record Stop(float offset, List<Declaration> declarations) {
    }
}
