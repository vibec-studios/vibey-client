package dev.henny.vui.template;

import dev.henny.vui.diag.SourcePos;

public sealed interface CNode permits CElement, CText {
    SourcePos pos();
}
