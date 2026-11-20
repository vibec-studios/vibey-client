package dev.henny.vui.template;

import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.expr.CompiledExpr;
import dev.henny.vui.style.Declaration;

import java.util.List;
import java.util.Map;

public record CElement(String tag, SourcePos pos, String id, String staticClass, List<Declaration> staticStyle,
                       Map<String, String> attrs, List<CBinding> bindings, List<CEvent> events, Branch branch,
                       CompiledExpr condition, ForSpec forSpec, CompiledExpr key, CompiledExpr show, CModel model,
                       String ref, SlotSpec slot, List<CNode> children) implements CNode {

    public boolean isTemplate() {
        return tag.equals("template");
    }

    public boolean isSlotOutlet() {
        return tag.equals("slot");
    }

    public String slotOutletName() {
        String n = attrs.get("name");
        return n == null ? "default" : n;
    }

    public CElement withoutFor() {
        return new CElement(tag, pos, id, staticClass, staticStyle, attrs, bindings, events, Branch.NONE, null, null,
                key, show, model, ref, slot, children);
    }

    public CElement withoutBranch() {
        return new CElement(tag, pos, id, staticClass, staticStyle, attrs, bindings, events, Branch.NONE, null, forSpec,
                key, show, model, ref, slot, children);
    }

    public CBinding binding(String name) {
        for (CBinding b : bindings) if (b.name().equals(name)) return b;
        return null;
    }
}
