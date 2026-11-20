package dev.henny.vibeyclient.module.inventory.search;

import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.mc.VuiMc;
import dev.henny.vui.mc.host.DocumentView;

public final class SearchOverlay {
    public static final String DOCUMENT = "vibeyclient:inventory/search";
    public static final int WIDTH = 96, HEIGHT = 14;

    private final SearchController controller = new SearchController();
    private final DocumentView view;

    public SearchOverlay(String initial) {
        controller.query.set(initial == null ? "" : initial);
        Document doc = VuiMc.vui().open(DOCUMENT, controller);
        view = new DocumentView(doc);
    }

    public SearchController controller() {
        return controller;
    }

    public Document document() {
        return view.document();
    }

    public void draw(Object graphics, int mouseX, int mouseY, int width, int height, float x, float y) {
        controller.place(x, y);
        SearchState.query(controller.query.peek());
        controller.show(SearchState.count(), SearchState.active());
        view.move(mouseX, mouseY);
        view.draw(graphics, width, height, false);
    }

    public boolean hits(double x, double y) {
        return view.hits(x, y);
    }

    public boolean down(double x, double y, int button) {
        if (!view.hits(x, y)) {
            blur();
            return false;
        }
        view.down(x, y, button);
        return true;
    }

    public boolean up(double x, double y, int button) {
        return view.up(x, y, button) && view.hits(x, y);
    }

    public boolean focused() {
        return view.document().events().focus().focused() != null;
    }

    public void focus() {
        Element field = view.document().find("q");
        if (field != null) view.document().events().focus().focus(field, true);
    }

    public void blur() {
        view.document().events().focus().blur();
    }

    public boolean key(int key, int scancode) {
        return view.keyDown(key, scancode);
    }

    public boolean typed(int codepoint) {
        return focused() && view.typed(codepoint);
    }

    public void close() {
        VuiMc.vui().close(view.document());
    }
}
