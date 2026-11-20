package dev.henny.vui.dom;

import dev.henny.vui.bind.Effect;
import dev.henny.vui.component.Behavior;
import dev.henny.vui.diag.ErrorSink;
import dev.henny.vui.dom.build.BuildContext;
import dev.henny.vui.dom.build.BuildHost;
import dev.henny.vui.dom.build.TreeBuilder;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.template.CompiledTemplate;

import java.util.Map;

final class DocumentBuilder implements BuildHost {
    private final Document document;

    DocumentBuilder(Document document) {
        this.document = document;
    }

    void build(Element root, CompiledTemplate template) {
        TreeBuilder tree = new TreeBuilder(this);
        tree.buildInto(root, template.roots(), document.rootScope(), new BuildContext(template, Map.of()));
    }

    @Override
    public Behavior behavior(String tag) {
        return document.vui().behaviors().get(tag);
    }

    @Override
    public CompiledTemplate component(String id) {
        return document.vui().component(id);
    }

    @Override
    public Scope rootScope() {
        return document.rootScope();
    }

    @Override
    public ErrorSink errors() {
        return document.errors();
    }

    @Override
    public Effect effect(Runnable body) {
        return document.effects().create(body);
    }

    @Override
    public void afterHandler() {
        document.afterHandler();
    }

    @Override
    public void registerRef(String name, Element element) {
        element.ref(name);
        document.hooks().registerRef(name, element);
    }

    @Override
    public void useComponentStyles(CompiledTemplate template) {
        document.hooks().useComponent(template);
        document.styles().addComponentSheet(template.id(), template.styles());
    }

    @Override
    public boolean mounted() {
        return document.isMounted();
    }
}
