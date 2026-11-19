package dev.henny.r3.panel;

import dev.henny.r3.object.ObjectContent;
import dev.henny.r3.render.PanelCanvas;

public interface PanelContent extends ObjectContent, PointerContent {
    void layout(int width, int height);

    void draw(PanelCanvas canvas);
}
