package dev.henny.vui.dom;

import dev.henny.vui.diag.SourcePos;

final class TooltipBubble {
    private TooltipBubble() {
    }

    static Element create(Document doc) {
        SourcePos pos = new SourcePos("<vui:tooltip>", 1, 1);
        Element bubble = new Element("tooltip-bubble", doc.vui().behaviors().get("tooltip-bubble"), pos);
        bubble.flags().internal = true;
        Element text = new Element("text", doc.vui().behaviors().get("text"), pos);
        text.flags().internal = true;
        bubble.sources().add(new StaticChild(text));
        bubble.rebuildChildren();
        doc.root().sources().add(new StaticChild(bubble));
        doc.root().rebuildChildren();
        return bubble;
    }
}
