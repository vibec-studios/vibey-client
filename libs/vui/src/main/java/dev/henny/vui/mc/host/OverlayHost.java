package dev.henny.vui.mc.host;

import dev.henny.vui.Vui;
import dev.henny.vui.dom.Document;
import dev.henny.vui.mc.compat.Mc;

import java.util.ArrayList;
import java.util.List;

public final class OverlayHost {
    private final Vui vui;
    private final List<DocumentView> views = new ArrayList<>();

    public OverlayHost(Vui vui) {
        this.vui = vui;
    }

    public Document show(String documentId, Object controller) {
        Document doc = vui.open(documentId, controller);
        doc.onEscape(() -> close(doc));
        views.add(new DocumentView(doc));
        return doc;
    }

    public void close(Document doc) {
        if (views.removeIf(v -> v.document() == doc)) vui.close(doc);
    }

    public void closeAll() {
        for (DocumentView v : List.copyOf(views)) close(v.document());
    }

    public List<Document> documents() {
        List<Document> out = new ArrayList<>(views.size());
        for (DocumentView v : views) out.add(v.document());
        return out;
    }

    public Document top() {
        return views.isEmpty() ? null : views.get(views.size() - 1).document();
    }

    DocumentView hit(double x, double y) {
        for (int i = views.size() - 1; i >= 0; i--) if (views.get(i).hits(x, y)) return views.get(i);
        return null;
    }

    void draw(Object graphics, int mouseX, int mouseY) {
        for (DocumentView v : List.copyOf(views)) {
            if (mouseX >= 0) v.move(mouseX, mouseY);
            v.draw(graphics, Mc.guiWidth(), Mc.guiHeight(), false);
        }
    }

    boolean down(double x, double y, int button) {
        DocumentView hit = hit(x, y);
        for (DocumentView v : List.copyOf(views)) if (v != hit) v.down(x, y, button);
        if (hit == null) return false;
        hit.down(x, y, button);
        return true;
    }

    boolean up(double x, double y, int button) {
        boolean used = hit(x, y) != null;
        for (DocumentView v : List.copyOf(views)) v.up(x, y, button);
        return used;
    }

    boolean scroll(double x, double y, double dx, double dy) {
        DocumentView hit = hit(x, y);
        return hit != null && hit.scroll(x, y, dx, dy);
    }

    boolean key(int key, int scancode) {
        if (views.isEmpty()) return false;
        return views.get(views.size() - 1).keyDown(key, scancode);
    }
}
