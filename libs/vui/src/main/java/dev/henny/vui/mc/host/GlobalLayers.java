package dev.henny.vui.mc.host;

import dev.henny.vui.mc.compat.Mc;
import dev.henny.vui.mc.dev.Inspector;
import dev.henny.vui.mc.draw.McCanvas;

public final class GlobalLayers {
    private final HudHost hud;
    private final ToastHost toasts;
    private final OverlayHost overlays;
    private final Inspector inspector;
    private final McCanvas canvas;
    private long frames;

    public GlobalLayers(HudHost hud, ToastHost toasts, OverlayHost overlays, Inspector inspector, McCanvas canvas) {
        this.hud = hud;
        this.toasts = toasts;
        this.overlays = overlays;
        this.inspector = inspector;
        this.canvas = canvas;
    }

    public long frames() {
        return frames;
    }

    public void onHud(Object graphics) {
        frames++;
        hud.renderInGame(graphics);
        if (Mc.screen() != null || Mc.hudHidden()) return;
        layer(graphics);
        toasts.draw(graphics, -1, -1);
        overlays.draw(graphics, -1, -1);
    }

    public void afterScreen(Object graphics, int mouseX, int mouseY) {
        frames++;
        layer(graphics);
        toasts.draw(graphics, mouseX, mouseY);
        overlays.draw(graphics, mouseX, mouseY);
        inspector.draw(graphics, mouseX, mouseY);
    }

    public boolean click(double x, double y, int button) {
        return overlays.down(x, y, button) || toasts.click(x, y, button);
    }

    public boolean release(double x, double y, int button) {
        return overlays.up(x, y, button);
    }

    public boolean scroll(double x, double y, double dx, double dy) {
        return overlays.scroll(x, y, dx, dy);
    }

    public boolean key(int key, int scancode) {
        return inspector.key(key) || overlays.key(key, scancode);
    }

    private void layer(Object graphics) {
        canvas.begin(graphics);
        canvas.layer();
        canvas.end();
    }
}
