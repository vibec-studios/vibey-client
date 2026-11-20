package dev.henny.vui.dom.build;

import dev.henny.vui.bind.Effect;
import dev.henny.vui.component.Behavior;
import dev.henny.vui.diag.ErrorSink;
import dev.henny.vui.dom.Element;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.template.CompiledTemplate;

public interface BuildHost {
    Behavior behavior(String tag);

    CompiledTemplate component(String id);

    Scope rootScope();

    ErrorSink errors();

    Effect effect(Runnable body);

    void afterHandler();

    void registerRef(String name, Element element);

    void useComponentStyles(CompiledTemplate template);

    boolean mounted();
}
