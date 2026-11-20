package dev.henny.vui.paint;

import dev.henny.vui.diag.Diagnostic;
import dev.henny.vui.dom.Document;
import dev.henny.vui.render.Canvas;

import java.util.ArrayList;
import java.util.List;

public final class DiagnosticsOverlay {
    private static final int MAX_LINES = 8;
    private static final int BACKGROUND = 0xE0140008;
    private static final int BORDER = 0xFFFF5470;
    private static final int TEXT = 0xFFFFD6DC;

    private final List<String> lines = new ArrayList<>();
    private Object cachedTemplate;
    private int cachedVersion = -1;

    public void paint(Document doc, PaintContext ctx, float width, float height) {
        refresh(doc);
        if (lines.isEmpty()) return;
        Canvas c = ctx.canvas();
        float lineHeight = ctx.metrics().lineHeight() + 1;
        float panelH = lines.size() * lineHeight + 8;
        float x = 4;
        float y = 4;
        float w = Math.max(40, width - 8);
        c.layer();
        c.fill(x, y, w, panelH, BACKGROUND);
        c.fill(x, y, w, 1, BORDER);
        c.fill(x, y + panelH - 1, w, 1, BORDER);
        c.fill(x, y, 1, panelH, BORDER);
        c.fill(x + w - 1, y, 1, panelH, BORDER);
        c.pushScissor(x, y, w, panelH);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            c.text(line, 0, line.length(), x + 4, y + 4 + i * lineHeight, TEXT, false, false);
        }
        c.popScissor();
    }

    private void refresh(Document doc) {
        if (cachedTemplate == doc.template() && cachedVersion == doc.errors().version()) return;
        cachedTemplate = doc.template();
        cachedVersion = doc.errors().version();
        lines.clear();
        List<Diagnostic> all = doc.diagnostics().errors();
        if (all.isEmpty()) return;
        lines.add("vui: " + all.size() + " error" + (all.size() == 1 ? "" : "s"));
        for (int i = 0; i < all.size() && lines.size() < MAX_LINES; i++) {
            Diagnostic d = all.get(i);
            lines.add(d.pos().shortText() + "  " + d.message());
            if (d.hint() != null && lines.size() < MAX_LINES) lines.add("    → " + d.hint());
        }
    }
}
