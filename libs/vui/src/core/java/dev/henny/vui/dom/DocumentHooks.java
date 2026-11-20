package dev.henny.vui.dom;

import dev.henny.vui.template.CompiledTemplate;

final class DocumentHooks {
    private final Document document;

    DocumentHooks(Document document) {
        this.document = document;
    }

    void entering(Element e) {
        if (document.styles().hasEnterRules()) document.mounts().entering(e);
        else e.flags().entering = false;
    }

    void registerRef(String name, Element e) {
        document.mounts().ref(name, e);
    }

    void useComponent(CompiledTemplate t) {
        if (document.mounts().useComponent(t)) document.errors().touch();
    }

    void registerMounted(Element e) {
        if (e.behavior().overlay(e)) document.overlays().register(e);
    }

    void forget(Element e) {
        document.events().forget(e);
        document.overlays().forget(e);
        document.styles().forget(e);
        document.mounts().forget(e);
    }

    Element createTooltipBubble() {
        return TooltipBubble.create(document);
    }
}
