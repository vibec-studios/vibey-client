package dev.henny.vibeyclient.ui.hud.render;

import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vui.hud.HudLayer;
import dev.henny.vui.mc.VuiRuntime;
import dev.henny.vui.mc.draw.McCanvas;

import java.util.List;

public final class HudRenderer {
    private static HudRenderer instance;

    private final HudLayer layer = new HudLayer();
    private final ViewSet views = new ViewSet(layer);
    private final HudLayout.Box box = new HudLayout.Box();
    private long lastNanos;
    private boolean failed;

    public static HudRenderer get() {
        if (instance == null) instance = new HudRenderer();
        return instance;
    }

    public HudLayer layer() {
        return layer;
    }

    public List<ElementView> views() {
        return views.list();
    }

    public void renderGame(Gfx g) {
        frame(g, false);
    }

    public void renderEditor(Gfx g) {
        frame(g, true);
    }

    private void frame(Gfx g, boolean editor) {
        if (failed) return;
        try {
            views.sync();
            if (!syncAll(g, editor)) return;
            draw(g, editor);
        } catch (RuntimeException | LinkageError e) {
            failed = true;
            dev.henny.vibeyclient.Vibey.LOG.error("HUD layer failed; widgets are off until restart", e);
        }
    }

    private boolean syncAll(Gfx g, boolean editor) {
        int sw = g.screenWidth();
        int sh = g.screenHeight();
        long now = System.currentTimeMillis();
        boolean hideAll = !editor && HudLayout.widgetsHiddenNow();
        boolean any = false;
        boolean resized = false;
        for (ElementView v : views.list()) {
            float w = v.width();
            float h = v.height();
            any |= v.sync(g, editor, hideAll, sw, sh, now, box);
            resized |= w != v.width() || h != v.height();
        }
        if (layer.width() != sw || layer.height() != sh) layer.resize(sw, sh);
        else if (resized) layer.layout();
        return any;
    }

    private void draw(Gfx g, boolean editor) {
        HudFrame.begin(g, System.currentTimeMillis(), editor);
        McCanvas canvas = VuiRuntime.get().canvas().begin(g.g);
        try {
            layer.frame(dt());
            layer.render(canvas);
        } finally {
            canvas.end();
            HudFrame.end();
        }
    }

    private float dt() {
        long n = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : Math.min(0.25f, (n - lastNanos) / 1e9f);
        lastNanos = n;
        return dt;
    }

    public ElementView view(HudElement e) {
        return views.of(e);
    }
}
