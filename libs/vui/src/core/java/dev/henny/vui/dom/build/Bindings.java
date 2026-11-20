package dev.henny.vui.dom.build;

import dev.henny.vui.bind.Effect;
import dev.henny.vui.dom.Element;
import dev.henny.vui.event.Handler;
import dev.henny.vui.expr.CompiledExpr;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.expr.Values;
import dev.henny.vui.expr.VarScope;
import dev.henny.vui.style.Declaration;
import dev.henny.vui.template.CBinding;
import dev.henny.vui.template.CElement;
import dev.henny.vui.template.CEvent;
import dev.henny.vui.template.CModel;
import dev.henny.vui.template.StyleAttributes;

import java.util.ArrayList;
import java.util.List;

public final class Bindings {
    private final BuildHost host;

    Bindings(BuildHost host) {
        this.host = host;
    }

    public void install(Element e, CElement node, Scope scope) {
        List<CBinding> styleBindings = new ArrayList<>();
        for (CBinding b : node.bindings()) {
            if (b.name().equals("style") || StyleAttributes.isStyle(b.name())) styleBindings.add(b);
            else bind(e, b, scope);
        }
        if (!styleBindings.isEmpty()) bindStyle(e, styleBindings, scope);
        if (node.key() != null && !node.key().isConstant()) e.key(node.key().evaluate(scope, host.errors()));
        if (node.show() != null) bindShow(e, node.show(), scope);
        if (node.model() != null) bindModel(e, node.model(), scope);
        for (CEvent ev : node.events()) listen(e, ev, scope);
    }

    public void bind(Element e, CBinding b, Scope scope) {
        CompiledExpr expr = b.expr();
        String name = b.name();
        run(e, () -> {
            Object v = expr.evaluate(scope, host.errors());
            switch (name) {
                case "class" -> {
                    if (e.classes().setBound(ClassBinding.names(v))) e.markSubtreeStyleDirty();
                }
                case "id" -> e.id(v == null ? null : Values.display(v));
                default -> e.prop(name, v);
            }
        });
    }

    private void bindStyle(Element e, List<CBinding> styleBindings, Scope scope) {
        run(e, () -> {
            List<Declaration> all = new ArrayList<>();
            for (CBinding b : styleBindings) {
                Object v = b.expr().evaluate(scope, host.errors());
                if (b.name().equals("style")) all.addAll(StyleBinding.declarations(v, b.expr().pos(), host.errors()));
                else if (v != null) all.add(new Declaration(b.name(), Values.display(v), b.expr().pos()));
            }
            e.boundStyle(all);
        });
    }

    private void bindShow(Element e, CompiledExpr show, Scope scope) {
        run(e, () -> {
            boolean visible = Values.truthy(show.evaluate(scope, host.errors()));
            if (visible == e.flags().shown) return;
            e.flags().shown = visible;
            e.markLayoutDirty();
            if (e.parent() != null && e.parent().text() != null) e.parent().text().invalidate();
        });
    }

    private void bindModel(Element e, CModel model, Scope scope) {
        String prop = model.prop().isEmpty() ? e.behavior().modelProp() : model.prop();
        run(e, () -> e.prop(prop, model.target().evaluate(scope, host.errors())));
        e.addHandler(new Handler("update:" + prop, List.of(), event -> {
            Object value = event.detail();
            if (model.trim() && value != null) value = Values.display(value).trim();
            if (model.number()) value = Values.toNumber(value);
            model.target().assign(scope, value, host.errors());
            host.afterHandler();
        }));
    }

    public void listen(Element e, CEvent ev, Scope scope) {
        CompiledExpr handler = ev.handler();
        e.addHandler(new Handler(ev.name(), ev.modifiers(), event -> {
            Object payload = event.detail() != null ? event.detail() : event;
            handler.evaluate(VarScope.of(scope, "$event", payload), host.errors());
            host.afterHandler();
        }));
    }

    private void run(Element e, Runnable body) {
        Effect effect = host.effect(body);
        e.effects().add(effect);
        effect.run();
    }
}
