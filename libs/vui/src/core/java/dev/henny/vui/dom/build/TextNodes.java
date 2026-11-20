package dev.henny.vui.dom.build;

import dev.henny.vui.bind.Effect;
import dev.henny.vui.dom.Element;
import dev.henny.vui.dom.StaticChild;
import dev.henny.vui.expr.CompiledExpr;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.expr.Values;
import dev.henny.vui.template.CText;

import java.util.Set;

public final class TextNodes {
    private static final Set<String> TEXT_PARENTS = Set.of("text", "span", "b", "i");

    private TextNodes() {
    }

    public static boolean holdsRawText(String tag) {
        return TEXT_PARENTS.contains(tag);
    }

    public static Element create(TreeBuilder builder, Element parent, CText node, Scope scope) {
        Element raw = raw(builder, node, scope);
        if (parent != null && holdsRawText(parent.tag())) return raw;
        Element wrapper = new Element("text", builder.host().behavior("text"), node.pos());
        wrapper.scope(scope);
        wrapper.sources().add(new StaticChild(raw));
        wrapper.rebuildChildren();
        return wrapper;
    }

    private static Element raw(TreeBuilder builder, CText node, Scope scope) {
        Element e = new Element(Element.TEXT_TAG, builder.host().behavior(Element.TEXT_TAG), node.pos());
        e.scope(scope);
        if (node.isStatic()) {
            e.prop("text", node.staticText());
            return e;
        }
        Effect effect = builder.host().effect(() -> e.prop("text", render(node, scope, builder)));
        e.effects().add(effect);
        effect.run();
        return e;
    }

    private static String render(CText node, Scope scope, TreeBuilder builder) {
        StringBuilder sb = new StringBuilder();
        for (Object part : node.parts()) {
            if (part instanceof String s) sb.append(s);
            else sb.append(Values.display(((CompiledExpr) part).evaluate(scope, builder.host().errors())));
        }
        return sb.toString();
    }
}
