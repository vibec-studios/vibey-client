package dev.henny.vui.markup;

import dev.henny.vui.contract.Contract;
import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourcePos;

import java.util.List;
import java.util.Map;

public record ComponentFile(String id, String fileName, List<TemplateNode> template, String style,
                            SourcePos stylePos, Map<String, Object> props, Map<String, String> components,
                            Diagnostics diagnostics, Contract contract) {

    public ComponentFile(String id, String fileName, List<TemplateNode> template, String style, SourcePos stylePos,
                         Map<String, Object> props, Map<String, String> components, Diagnostics diagnostics) {
        this(id, fileName, template, style, stylePos, props, components, diagnostics, Contract.EMPTY);
    }

    public boolean hasErrors() {
        return diagnostics.hasErrors();
    }
}
