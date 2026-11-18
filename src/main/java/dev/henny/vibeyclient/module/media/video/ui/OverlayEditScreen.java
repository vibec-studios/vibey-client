package dev.henny.vibeyclient.module.media.video.ui;

import dev.henny.vibeyclient.module.media.video.HudElement;
import dev.henny.vibeyclient.module.media.video.Video;
import dev.henny.vibeyclient.ui.BaseScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Minimal editor for one {@link HudElement}: drag to move, mouse wheel to scale, Esc to finish. Stand-in until the
 * Media module's generic HUD editor ({@code ui/hud}) takes the elements from {@code VideoFeature.hudElements()}.
 */
public final class OverlayEditScreen extends BaseScreen {
    private final Screen parent;
    private final HudElement el;
    private boolean dragging;
    private double grabX, grabY;

    public OverlayEditScreen(Screen parent, HudElement el) {
        super(Component.literal(Video.tr("overlay.edit")));
        this.parent = parent;
        this.el = el;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int[] box() {
        float s = el.scale();
        int w = Math.round(el.width() * s);
        int h = Math.round(el.height() * s);
        int x = Math.round(el.x() * Math.max(0, width - w));
        int y = Math.round(el.y() * Math.max(0, height - h));
        return new int[] {x, y, w, h};
    }

    @Override
    protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
        g.rect(0, 0, width, height, 0x30000000);
        int[] b = box();
        el.render(g, b[0], b[1], el.scale(), true);
        String hint = Video.tr("overlay.hint");
        int w = g.width(hint) + 16;
        g.rect((width - w) / 2, height - 40, w, 18, 0xC0101014);
        g.text(hint, (width - w) / 2 + 8, height - 35, Theme.TEXT);
    }

    @Override
    protected boolean click(double x, double y, int button) {
        int[] b = box();
        if (button == 0 && x >= b[0] && x < b[0] + b[2] && y >= b[1] && y < b[1] + b[3]) {
            dragging = true;
            grabX = x - b[0];
            grabY = y - b[1];
            return true;
        }
        return false;
    }

    @Override
    protected boolean drag(double x, double y, int button) {
        if (!dragging) return false;
        int[] b = box();
        float fx = (float) ((x - grabX) / Math.max(1, width - b[2]));
        float fy = (float) ((y - grabY) / Math.max(1, height - b[3]));
        el.setLayout(fx, fy, el.scale());
        return true;
    }

    @Override
    protected boolean release(double x, double y, int button) {
        dragging = false;
        return true;
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        el.setLayout(el.x(), el.y(), el.scale() * (amount > 0 ? 1.1f : 1 / 1.1f));
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
