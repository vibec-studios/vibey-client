package dev.henny.vui.component;

import dev.henny.vui.dom.Element;
import dev.henny.vui.dom.build.SlotContent;
import dev.henny.vui.dom.build.TreeBuilder;

public interface TemplatedBehavior extends Behavior {
    void template(Element e, SlotContent content, TreeBuilder builder);
}
