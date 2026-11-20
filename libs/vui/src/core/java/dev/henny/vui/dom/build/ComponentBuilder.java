package dev.henny.vui.dom.build;

import dev.henny.vui.bind.Action;
import dev.henny.vui.dom.Element;
import dev.henny.vui.dom.StaticChild;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.expr.VarScope;
import dev.henny.vui.template.Branch;
import dev.henny.vui.template.CElement;
import dev.henny.vui.template.CNode;
import dev.henny.vui.template.CompiledTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ComponentBuilder {
    private final TreeBuilder tree;

    ComponentBuilder(TreeBuilder tree) {
        this.tree = tree;
    }

    public Element build(CElement usage, String componentId, Scope usageScope, BuildContext usageCtx) {
        BuildHost host = tree.host();
        CompiledTemplate t = host.component(componentId);
        if (t == null || t.roots().isEmpty() && t.hasErrors()) {
            if (t != null) host.useComponentStyles(t);
            return missing(usage, componentId);
        }
        host.useComponentStyles(t);
        List<String> propNames = new ArrayList<>(t.props().keySet());
        Element[] hostRef = new Element[1];
        String[] names = new String[propNames.size() + 1];
        Object[] defaults = new Object[names.length];
        for (int i = 0; i < propNames.size(); i++) {
            names[i] = propNames.get(i);
            defaults[i] = t.props().get(propNames.get(i));
        }
        names[propNames.size()] = "$emit";
        defaults[propNames.size()] = (Action) args -> emit(hostRef[0], args);
        VarScope props = new VarScope(host.rootScope(), names, defaults);
        Fallthrough fallthrough = Fallthrough.split(usage, propNames);
        Element root = buildRoot(t, props, Slots.collect(usage, usageScope, usageCtx));
        hostRef[0] = root;
        fallthrough.bindProps(this, root, props, usageScope, propNames);
        fallthrough.apply(tree, root, usageScope);
        return root;
    }

    TreeBuilder tree() {
        return tree;
    }

    private Element buildRoot(CompiledTemplate t, Scope props, Map<String, SlotContent> slots) {
        BuildContext ctx = new BuildContext(t, slots);
        List<CNode> roots = t.roots();
        if (roots.size() == 1 && roots.get(0) instanceof CElement single && single.branch() == Branch.NONE
                && single.forSpec() == null && !single.isTemplate() && !single.isSlotOutlet()) {
            return tree.buildElement(single, props, ctx);
        }
        Element wrapper = new Element("column", tree.host().behavior("column"), roots.isEmpty() ? null : roots.get(0).pos());
        wrapper.scope(props);
        tree.buildInto(wrapper, roots, props, ctx);
        return wrapper;
    }

    private static Object emit(Element host, Object[] args) {
        if (host == null || args.length == 0) return null;
        Object detail = args.length > 1 ? args[1] : null;
        host.emit(String.valueOf(args[0]), detail);
        return null;
    }

    private Element missing(CElement usage, String id) {
        tree.host().errors().report(usage.pos(), "component '" + id + "' for <" + usage.tag() + "> could not be loaded",
                "check the import src and that the file exists");
        Element e = new Element("box", tree.host().behavior("box"), usage.pos());
        e.classes().addFixed("vui-missing");
        Element label = new Element(Element.TEXT_TAG, tree.host().behavior(Element.TEXT_TAG), usage.pos());
        label.prop("text", "missing <" + usage.tag() + ">");
        Element text = new Element("text", tree.host().behavior("text"), usage.pos());
        text.sources().add(new StaticChild(label));
        text.rebuildChildren();
        e.sources().add(new StaticChild(text));
        e.rebuildChildren();
        return e;
    }
}
