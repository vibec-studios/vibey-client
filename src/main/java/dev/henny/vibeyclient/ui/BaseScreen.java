package dev.henny.vibeyclient.ui;

import dev.henny.vibeyclient.client.Keys;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else
import net.minecraft.client.gui.GuiGraphics;
//? if >=1.21.9 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}

/**
 * Version glue for screens: subclasses implement {@link #draw}, {@link #click}, {@link #scroll} (and optionally
 * {@link #release}, {@link #drag}, {@link #key}) and never touch the per-version render/input signatures.
 * Ported from Vibey Lyrics.
 */
public abstract class BaseScreen extends Screen {
    protected BaseScreen(Component title) {
        super(title);
    }

    protected abstract void draw(Gfx gfx, int mouseX, int mouseY, float delta);

    /** @return true if the click was used */
    protected abstract boolean click(double x, double y, int button);

    protected abstract boolean scroll(double x, double y, double amount);

    protected boolean release(double x, double y, int button) {
        return false;
    }

    protected boolean drag(double x, double y, int button) {
        return false;
    }

    /** Drag with the movement since the last call (GUI pixels); defaults to {@link #drag(double, double, int)}. */
    protected boolean drag(double x, double y, int button, double dx, double dy) {
        return drag(x, y, button);
    }

    /** A typed character (only when no text field has the focus is it useful); true if used. */
    protected boolean typed(int codepoint) {
        return false;
    }

    /** Draws the vanilla title panorama (the rotating world background), full screen. */
    protected void panorama(Gfx gfx, float delta) {
        //? if >=26.1 {
        /*extractPanorama(gfx.g, delta);
        *///?} else
        renderPanorama(gfx.g, delta);
    }

    /** @return true if the key was used (called before the focused text field sees it) */
    protected boolean key(int keyCode) {
        return false;
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        draw(new Gfx(g, this.font), mouseX, mouseY, delta);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
    }
    *///?} else {
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        draw(new Gfx(g, this.font), mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
    }
    //?}

    // Buttons reach click/release/drag as GLFW numbers (0 left, 1 right, 2 middle) on every version: 26.3 (SDL)
    // reports 1 left, 2 middle, 3 right, see Keys.mouseFromNative. The vanilla super calls keep the native event.
    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (click(event.x(), event.y(), Keys.mouseFromNative(event.button()))) return true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (release(event.x(), event.y(), Keys.mouseFromNative(event.button()))) return true;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (drag(event.x(), event.y(), Keys.mouseFromNative(event.button()), dx, dy)) return true;
        return super.mouseDragged(event, dx, dy);
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double x, double y, int button) {
        if (click(x, y, button)) return true;
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (release(x, y, button)) return true;
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (drag(x, y, button, dx, dy)) return true;
        return super.mouseDragged(x, y, button, dx, dy);
    }
    *///?}

    //? if >=1.21.9 {
    @Override
    public boolean keyPressed(KeyEvent event) {
        // our screens compare GLFW codes; 26.3 reports SDL scancodes (Keys.fromNative is the identity before)
        if (key(Keys.fromNative(event.key()))) return true;
        return super.keyPressed(event);
    }
    //?} else {
    /*@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (key(keyCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    *///?}

    //? if >=1.21.9 {
    @Override
    public boolean charTyped(CharacterEvent event) {
        if (typed(event.codepoint())) return true;
        return super.charTyped(event);
    }
    //?} else {
    /*@Override
    public boolean charTyped(char c, int modifiers) {
        if (typed(c)) return true;
        return super.charTyped(c, modifiers);
    }
    *///?}

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        if (scroll(x, y, v)) return true;
        return super.mouseScrolled(x, y, h, v);
    }
}
