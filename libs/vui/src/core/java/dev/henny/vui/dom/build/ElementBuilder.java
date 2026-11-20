package dev.henny.vui.dom.build;

import dev.henny.vui.component.Behavior;
import dev.henny.vui.component.TemplatedBehavior;
import dev.henny.vui.dom.Element;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.template.CElement;
import dev.henny.vui.template.StyleAttributes;

import java.util.Map;

public final class ElementBuilder {
    private final TreeBuilder tree;
    private final Bindings bindings;

    ElementBuilder(TreeBuilder tree) {
        this.tree = tree;
        this.bindings = new Bindings(tree.host());
    }

    public Bindings bindings() {
        return bindings;
    }

    public Element build(CElement node, Scope scope, BuildContext ctx) {
        Behavior behavior = tree.host().behavior(node.tag());
        Element e = new Element(node.tag(), behavior, node.pos());
        e.scope(scope);
        applyStatic(e, node);
        bindings.install(e, node, scope);
        if (behavior instanceof TemplatedBehavior templated) {
            SlotContent slot = Slots.collect(node, scope, ctx).get("default");
            templated.template(e, slot != null ? slot : new SlotContent(java.util.List.of(), scope, ctx, node.slot()), tree);
        } else {
            tree.buildInto(e, node.children(), scope, ctx);
        }
        return e;
    }

    public void applyStatic(Element e, CElement node) {
        if (node.id() != null) e.id(node.id());
        if (node.staticClass() != null) e.classes().addFixed(node.staticClass());
        if (!node.staticStyle().isEmpty()) e.staticStyle(node.staticStyle());
        for (Map.Entry<String, String> a : node.attrs().entrySet()) {
            if (!StyleAttributes.isStyle(a.getKey())) e.prop(a.getKey(), StaticValues.parse(a.getValue()));
        }
        if (node.ref() != null) tree.host().registerRef(node.ref(), e);
        if (node.key() != null && node.key().isConstant()) e.key(node.key().evaluate(scope(e), tree.host().errors()));
    }

    private static Scope scope(Element e) {
        return e.scope();
    }
}
