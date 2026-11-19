package dev.henny.vui.markup;

import dev.henny.vui.diag.SourcePos;

public record TextPart(boolean expression, String text, SourcePos pos) {
}
