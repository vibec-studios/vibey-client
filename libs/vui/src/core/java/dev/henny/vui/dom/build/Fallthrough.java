package dev.henny.vui.dom.build;

import dev.henny.vui.bind.Effect;
import dev.henny.vui.dom.Element;
import dev.henny.vui.event.Handler;
import dev.henny.vui.expr.Coerce;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.expr.VarScope;
import dev.henny.vui.style.Declaration;
import dev.henny.vui.template.Branch;
import dev.henny.vui.template.CBinding;
import dev.henny.vui.template.CElement;
import dev.henny.vui.template.CModel;
import dev.henny.vui.template.StyleAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Fallthrough {
    private final CElement usage;
    private final Map<String, String> propAttrs = new LinkedHashMap<>();
    private final Map<String, String> otherAttrs = new LinkedHashMap<>();
    private final List<CBinding> propBindings = new ArrayList<>();
    private final List<CBinding> otherBindings = new ArrayList<>();
    private final List<Declaration> style = new ArrayList<>();

    private Fallthrough(CElement usage) {
        this.usage = usage;
    }

    static Fallthrough split(CElement usage, List<String> propNames) {
        Fallthrough f = new Fallthrough(usage);
        f.style.addAll(usage.staticStyle());
        for (Map.Entry<String, String> a : usage.attrs().entrySet()) {
            String prop = match(a.getKey(), propNames);
            if (prop != null) {
                f.propAttrs.put(prop, a.getValue());
                if (StyleAttributes.isStyle(a.getKey())) f.removeStyle(a.getKey(), a.getValue());
            } else if (!StyleAttributes.isStyle(a.getKey())) {
                f.otherAttrs.put(a.getKey(), a.getValue());
            }
        }
        for (CBinding b : usage.bindings()) {
            String prop = match(b.name(), propNames);
            if (prop != null) f.propBindings.add(new CBinding(prop, b.expr()));
            else f.otherBindings.add(b);
        }
        return f;
    }

    private void removeStyle(String attribute, String value) {
        String property = dev.henny.vui.style.PropertyNames.canonical(attribute);
        style.removeIf(d -> d.property().equals(property) && d.raw().equals(value.trim()));
    }

    List<Effect> bindProps(ComponentBuilder builder, VarScope props, Scope usageScope, List<String> propNames) {
        BuildHost host = builder.tree().host();
        List<Effect> effects = new ArrayList<>();
        for (Map.Entry<String, String> a : propAttrs.entrySet()) {
            Object current = props.lookup(a.getKey());
            Object parsed = StaticValues.parse(a.getValue());
            props.set(a.getKey(), current instanceof String ? a.getValue() : current == null ? parsed : Coerce.to(current.getClass(), parsed));
        }
        for (CBinding b : propBindings) {
            Effect effect = host.effect(() -> props.set(b.name(), b.expr().evaluate(usageScope, host.errors())));
            effects.add(effect);
            effect.run();
        }
        CModel model = usage.model();
        if (model == null) return effects;
        String prop = modelProp(model);
        if (!propNames.contains(prop)) {
            host.errors().report(usage.pos(), "v-model on <" + usage.tag() + "> needs a prop '" + prop + "'", "declare it in <props>");
            return effects;
        }
        Effect effect = host.effect(() -> props.set(prop, model.target().evaluate(usageScope, host.errors())));
        effects.add(effect);
        effect.run();
        return effects;
    }

    void attach(ComponentBuilder builder, Element root, List<Effect> effects, Scope usageScope, List<String> propNames) {
        root.effects().addAll(effects);
        CModel model = usage.model();
        if (model == null || !propNames.contains(modelProp(model))) return;
        BuildHost host = builder.tree().host();
        root.addHandler(new Handler("update:" + modelProp(model), List.of(), ev -> {
            model.target().assign(usageScope, ev.detail(), host.errors());
            host.afterHandler();
        }));
    }

    private static String modelProp(CModel model) {
        return model.prop().isEmpty() ? "value" : model.prop();
    }

    void apply(TreeBuilder tree, Element root, Scope usageScope) {
        if (usage.id() != null) root.id(usage.id());
        if (usage.staticClass() != null) root.classes().addFixed(usage.staticClass());
        if (!style.isEmpty()) {
            List<Declaration> merged = new ArrayList<>(root.inlineStyle());
            merged.addAll(style);
            root.staticStyle(merged);
        }
        for (Map.Entry<String, String> a : otherAttrs.entrySet()) root.prop(a.getKey(), StaticValues.parse(a.getValue()));
        if (usage.ref() != null) tree.host().registerRef(usage.ref(), root);
        CElement rest = new CElement(usage.tag(), usage.pos(), null, null, List.of(), Map.of(), otherBindings,
                usage.events(), Branch.NONE, null, null, usage.key(), usage.show(), null, null, null, List.of());
        tree.elements().bindings().install(root, rest, usageScope);
    }

    private static String match(String name, List<String> propNames) {
        if (propNames.contains(name)) return name;
        String camel = StyleBinding.camel(name);
        return propNames.contains(camel) ? camel : null;
    }
}
