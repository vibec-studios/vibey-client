package dev.henny.vui.dom;

import dev.henny.vui.paint.DiagnosticsOverlay;
import dev.henny.vui.paint.ElementPainter;
import dev.henny.vui.paint.PaintContext;
import dev.henny.vui.render.Canvas;

import java.util.List;

final class DocumentRenderer {
    private final Document document;
    private final ElementPainter painter = new ElementPainter();
    private final PaintContext context = new PaintContext();
    private final DiagnosticsOverlay diagnostics = new DiagnosticsOverlay();

    DocumentRenderer(Document document) {
        this.document = document;
    }

    void render(Canvas canvas) {
        PaintContext ctx = context.begin(canvas, document.vui().backend().text(), document.vui().backend().textures(),
                document.vui().theme(), document.time());
        ctx.focusVisible(document.events().focus().visible());
        float w = document.width();
        float h = document.height();
        painter.paintTree(document.root(), ctx, w, h);
        if (document.beforeOverlays() != null) document.beforeOverlays().accept(canvas);
        List<Element> open = document.overlays().open();
        for (int i = 0; i < open.size(); i++) {
            canvas.layer();
            painter.paintOverlay(open.get(i), ctx, w, h);
        }
        if (document.vui().showErrorOverlay()) diagnostics.paint(document, ctx, w, h);
    }
}
