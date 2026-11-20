package dev.henny.r3.vui;

import dev.henny.r3.panel.KeyContent;
import dev.henny.r3.panel.PanelContent;
import dev.henny.r3.panel.PointerButton;
import dev.henny.r3.render.PanelCanvas;
import dev.henny.vui.Vui;
import dev.henny.vui.component.builtin.TextFieldBehavior;
import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;

public final class VuiPanelContent implements PanelContent, KeyContent {
    private static final float OUTSIDE = -100_000f;

    private final Document document;
    private final VuiCanvas canvas;
    private String hoveredControl;

    public VuiPanelContent(Document document, TextureResolver textures) {
        this.document = document;
        this.canvas = new VuiCanvas(textures);
    }

    public static VuiPanelContent open(Vui vui, String documentId, Object controller, TextureResolver textures) {
        return new VuiPanelContent(vui.open(documentId, controller), textures);
    }

    public Document document() {
        return document;
    }

    public String hoveredControl() {
        return hoveredControl;
    }

    public void close() {
        document.vui().close(document);
    }

    @Override
    public void layout(int width, int height) {
        document.resize(width, height);
    }

    @Override
    public void update(float dt) {
        document.frame(dt);
    }

    @Override
    public void draw(PanelCanvas target) {
        document.render(canvas.target(target));
    }

    @Override
    public String controlAt(float x, float y) {
        return VuiControls.controlId(document.events().hit(x, y));
    }

    @Override
    public void hoverChanged(String controlId) {
        hoveredControl = controlId;
    }

    @Override
    public void pointerMove(float x, float y) {
        document.pointerMove(x, y);
    }

    @Override
    public void pointerExit() {
        document.pointerMove(OUTSIDE, OUTSIDE);
    }

    @Override
    public boolean pointerDown(float x, float y, PointerButton button) {
        return document.pointerDown(x, y, glfw(button), 0);
    }

    @Override
    public void pointerUp(float x, float y, PointerButton button) {
        document.pointerUp(x, y, glfw(button), 0);
        if (!keyFocused()) document.events().focus().blur();
    }

    @Override
    public boolean scroll(float x, float y, double amount) {
        Element hit = document.events().hit(x, y);
        float before = ScrollSum.of(hit);
        document.scroll(x, y, 0f, (float) amount, 0);
        return ScrollSum.of(hit) != before;
    }

    static int glfw(PointerButton button) {
        return switch (button) {
            case PRIMARY -> 0;
            case SECONDARY -> 1;
            case MIDDLE -> 2;
        };
    }

    @Override
    public boolean keyFocused() {
        Element focused = document.events().focus().focused();
        return focused != null && focused.behavior() instanceof TextFieldBehavior;
    }

    @Override
    public boolean keyDown(int key, int scancode, int mods, boolean repeat) {
        return document.events().keyDown(key, scancode, mods, repeat);
    }

    @Override
    public void keyUp(int key, int scancode, int mods) {
        document.keyUp(key, scancode, mods);
    }

    @Override
    public boolean charTyped(int codepoint, int mods) {
        return document.charTyped(codepoint, mods);
    }

    @Override
    public void releaseKeyFocus() {
        document.events().focus().blur();
    }

    public void focus(String elementId) {
        Element e = document.find(elementId);
        if (e != null) document.events().focus().focus(e, false);
    }
}
