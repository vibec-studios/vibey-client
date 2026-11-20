package dev.henny.vui.dom.build;

import dev.henny.vui.dom.ChildSource;
import dev.henny.vui.dom.Element;
import dev.henny.vui.dom.StaticChild;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.template.Branch;
import dev.henny.vui.template.CElement;
import dev.henny.vui.template.CNode;
import dev.henny.vui.template.CText;

import java.util.ArrayList;
import java.util.List;

public final class TreeBuilder {
    private final BuildHost host;
    private final ElementBuilder elements;
    private final ComponentBuilder components;

    public TreeBuilder(BuildHost host) {
        this.host = host;
        this.elements = new ElementBuilder(this);
        this.components = new ComponentBuilder(this);
    }

    public BuildHost host() {
        return host;
    }

    public void buildInto(Element parent, List<CNode> nodes, Scope scope, BuildContext ctx) {
        buildChildren(parent, nodes, scope, ctx, parent.sources());
        parent.rebuildChildren();
    }

    public void buildChildren(Element parent, List<CNode> nodes, Scope scope, BuildContext ctx, List<ChildSource> into) {
        for (int i = 0; i < nodes.size(); i++) {
            CNode n = nodes.get(i);
            if (n instanceof CText t) {
                into.add(new StaticChild(TextNodes.create(this, parent, t, scope)));
                continue;
            }
            CElement e = (CElement) n;
            if (e.branch() == Branch.IF) {
                List<CElement> chain = new ArrayList<>();
                chain.add(e);
                int j = i + 1;
                while (j < nodes.size() && nodes.get(j) instanceof CElement next
                        && (next.branch() == Branch.ELSE_IF || next.branch() == Branch.ELSE)) {
                    chain.add(next);
                    j++;
                    if (next.branch() == Branch.ELSE) break;
                }
                into.add(new IfBlock(this, parent, chain, scope, ctx));
                i = j - 1;
            } else if (e.branch() != Branch.NONE) {
                continue;
            } else if (e.forSpec() != null) {
                into.add(new ForBlock(this, parent, e, scope, ctx));
            } else if (e.isSlotOutlet()) {
                SlotOutlet.build(this, parent, e, scope, ctx, into);
            } else if (e.isTemplate() && e.slot() == null) {
                buildChildren(parent, e.children(), scope, ctx, into);
            } else if (!e.isTemplate()) {
                into.add(new StaticChild(buildElement(e, scope, ctx)));
            }
        }
    }

    public Element buildElement(CElement e, Scope scope, BuildContext ctx) {
        String componentId = ctx.componentId(e.tag());
        if (componentId != null) return components.build(e, componentId, scope, ctx);
        return elements.build(e, scope, ctx);
    }

    public ElementBuilder elements() {
        return elements;
    }
}
