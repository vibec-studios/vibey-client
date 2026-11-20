package dev.henny.vui.dom.build;

import dev.henny.vui.template.CompiledTemplate;

import java.util.Map;

public record BuildContext(CompiledTemplate template, Map<String, SlotContent> slots) {

    public String componentId(String tag) {
        return template == null ? null : template.components().get(tag);
    }

    public SlotContent slot(String name) {
        return slots == null ? null : slots.get(name);
    }
}
