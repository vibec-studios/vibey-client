package dev.henny.vui.template;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.style.StyleSheet;

import java.util.List;
import java.util.Map;

public record CompiledTemplate(String id, String fileName, List<CNode> roots, StyleSheet styles,
                               Map<String, Object> props, Map<String, String> components, Diagnostics diagnostics) {

    public boolean hasErrors() {
        return diagnostics.hasErrors();
    }
}
