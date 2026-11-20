package dev.henny.vui.markup;

import dev.henny.vui.diag.SourcePos;

public sealed interface TemplateNode permits ElementNode, TextNode {
    SourcePos pos();
}
