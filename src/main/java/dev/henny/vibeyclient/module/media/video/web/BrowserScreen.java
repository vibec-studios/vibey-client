package dev.henny.vibeyclient.module.media.video.web;

import dev.henny.vibeyclient.module.media.video.Video;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Use a web screen with mouse and keyboard (scroll a page, click the player's own controls, accept a page's
 * dialogs). The page is shown as large as fits; input is mapped to browser pixels. Esc leaves.
 */
final class BrowserScreen extends Screen {
    private final Screen parent;
    private final WebPlayer player;
    private final MCEFBrowser browser;
    private int rx, ry, rw, rh;

    BrowserScreen(Screen parent, WebPlayer player) {
        super(Component.literal(Video.tr("browser.title")));
        this.parent = parent;
        this.player = player;
        this.browser = player.browser();
    }

    @Override
    protected void init() {
        float a = player.width() / (float) player.height();
        rw = width - 40;
        rh = Math.round(rw / a);
        if (rh > height - 50) {
            rh = height - 50;
            rw = Math.round(rh * a);
        }
        rx = (width - rw) / 2;
        ry = 16 + (height - 50 - rh) / 2;
        browser.setFocus(true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float delta) {
        Gfx g = new Gfx(gg, font);
        g.rect(0, 0, width, height, 0xB0000000);
        g.rect(rx - 1, ry - 1, rw + 2, rh + 2, 0xFF0E0E12);
        Identifier tex = player.texture();
        if (tex != null) g.image(tex, rx, ry, rw, rh, 0, 0, player.width(), player.height(), player.width(), player.height());
        g.textCenter(Video.tr("browser.hint"), width / 2, height - 22, Theme.MUTED);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // drawn in render
    }

    private int bx(double x) {
        return (int) Math.round((x - rx) * player.width() / (double) rw);
    }

    private int by(double y) {
        return (int) Math.round((y - ry) * player.height() / (double) rh);
    }

    private MouseButtonEvent map(MouseButtonEvent e) {
        return new MouseButtonEvent(bx(e.x()), by(e.y()), new MouseButtonInfo(e.button(), e.modifiers()));
    }

    private boolean inside(double x, double y) {
        return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (!inside(event.x(), event.y())) return super.mouseClicked(event, doubled);
        browser.onMouseClicked(map(event), doubled);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        browser.onMouseReleased(map(event));
        return true;
    }

    @Override
    public void mouseMoved(double x, double y) {
        browser.onMouseMoved(bx(x), by(y));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        browser.onMouseMoved(bx(event.x()), by(event.y()));
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        if (inside(x, y)) browser.onMouseScrolled(bx(x), by(y), v);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        browser.onKeyPressed(event);
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        browser.onKeyReleased(event);
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        browser.onCharTyped(event);
        return true;
    }

    @Override
    public void removed() {
        browser.setFocus(false);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
