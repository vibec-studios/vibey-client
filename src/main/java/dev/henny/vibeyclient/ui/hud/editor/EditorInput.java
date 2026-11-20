package dev.henny.vibeyclient.ui.hud.editor;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.edit.EditorKeys;
import dev.henny.vibeyclient.ui.hud.edit.PointerTool;
import dev.henny.vui.dom.Document;
import dev.henny.vui.event.Keys;
import dev.henny.vui.mc.host.DocumentView;
import dev.henny.vui.mc.input.ModState;

final class EditorInput {
    private final HudEditorScreen screen;
    private final EditorController c;
    private final PointerTool tool;
    private boolean docPressed;
    private boolean toolPressed;

    EditorInput(HudEditorScreen screen, EditorController c, PointerTool tool) {
        this.screen = screen;
        this.c = c;
        this.tool = tool;
    }

    boolean overChrome(double x, double y) {
        DocumentView v = screen.view();
        if (v == null) return false;
        return overlaysOpen(v.document()) || v.hits(x, y);
    }

    private static boolean overlaysOpen(Document doc) {
        return !doc.overlays().open().isEmpty();
    }

    boolean move(double x, double y) {
        DocumentView v = screen.view();
        if (v != null) v.move(x, y);
        if (toolPressed) tool.drag(x, y, free(), ModState.shift());
        return true;
    }

    private boolean free() {
        return ModState.control() || ModState.alt() || !c.session.snapping();
    }

    boolean down(double x, double y, int button) {
        DocumentView v = screen.view();
        if (v == null) return false;
        if (overChrome(x, y)) {
            docPressed = true;
            v.down(x, y, button);
            return true;
        }
        if (button == 1) {
            ContextMenu.open(c, (float) x, (float) y, c.session.elementAt(x, y));
            return true;
        }
        toolPressed = tool.down(x, y, button, ModState.shift() || ModState.control());
        return true;
    }

    boolean up(double x, double y, int button) {
        DocumentView v = screen.view();
        if (docPressed && v != null) v.up(x, y, button);
        docPressed = false;
        if (toolPressed) tool.up();
        toolPressed = false;
        return true;
    }

    boolean wheel(double x, double y, double dx, double dy) {
        DocumentView v = screen.view();
        if (v != null && overChrome(x, y)) return v.scroll(x, y, dx, dy);
        if (dy == 0) return false;
        HudElement hit = c.session.elementAt(x, y);
        if (ModState.shift() && hit != null && hit.isWidget()) {
            c.session.history.checkpoint("widget-wheel");
            if (hit.widget().wheel(hit, dy > 0 ? 1 : -1)) return true;
        }
        c.actions.scaleStep(hit, dy > 0 ? 1 : -1);
        return true;
    }

    boolean keyDown(int key, int scancode) {
        DocumentView v = screen.view();
        if (v == null) return false;
        if (key == Keys.ESCAPE || overlaysOpen(v.document()) || focused(v.document())) {
            if (v.keyDown(key, scancode)) return true;
            if (key == Keys.ESCAPE) {
                escape();
                return true;
            }
        }
        if (overlaysOpen(v.document())) return v.keyDown(key, scancode);
        int letter = ClientCompat.layoutLetter(key);
        return EditorKeys.handle(c.session, c.actions, key, letter, ModState.control(), ModState.shift());
    }

    private static boolean focused(Document doc) {
        return doc.events().focus().focused() != null;
    }

    void escape() {
        if (!c.panel.peek().isEmpty()) c.panel.set("");
        else c.done();
    }
}
