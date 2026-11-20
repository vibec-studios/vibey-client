package dev.henny.vui.dom.build;

import dev.henny.vui.expr.Scope;
import dev.henny.vui.template.CNode;
import dev.henny.vui.template.SlotSpec;

import java.util.List;

public record SlotContent(List<CNode> nodes, Scope scope, BuildContext context, SlotSpec spec) {
}
