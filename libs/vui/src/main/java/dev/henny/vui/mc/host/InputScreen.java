package dev.henny.vui.mc.host;

import dev.henny.vui.mc.input.KeyCodes;
import dev.henny.vui.mc.input.MouseButtons;
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

public abstract class InputScreen extends Screen {
    protected InputScreen(Component title) {
        super(title);
    }

    protected abstract void draw(Object graphics, int mouseX, int mouseY, float delta);

    protected boolean vanillaBackground() {
        return false;
    }

    protected boolean pointerMove(double x, double y) {
        return false;
    }

    protected boolean pointerDown(double x, double y, int button) {
        return false;
    }

    protected boolean pointerUp(double x, double y, int button) {
        return false;
    }

    protected boolean wheel(double x, double y, double dx, double dy) {
        return false;
    }

    protected boolean keyDown(int key, int scancode) {
        return false;
    }

    protected boolean keyUp(int key, int scancode) {
        return false;
    }

    protected boolean typed(int codepoint) {
        return false;
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        draw(g, mouseX, mouseY, delta);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (vanillaBackground()) super.extractBackground(g, mouseX, mouseY, delta);
    }
    *///?} else {
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        //? if <1.21.6
        /*renderBackground(g, mouseX, mouseY, delta);*/
        draw(g, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (vanillaBackground()) super.renderBackground(g, mouseX, mouseY, delta);
    }
    //?}

    @Override
    public void mouseMoved(double x, double y) {
        pointerMove(x, y);
    }

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return pointerDown(event.x(), event.y(), MouseButtons.fromNative(event.button())) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return pointerUp(event.x(), event.y(), MouseButtons.fromNative(event.button())) || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return pointerMove(event.x(), event.y()) || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return keyDown(KeyCodes.fromNative(event.key()), scancode(event)) || super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return keyUp(KeyCodes.fromNative(event.key()), scancode(event)) || super.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return typed(event.codepoint()) || super.charTyped(event);
    }

    private static int scancode(KeyEvent event) {
        //? if >=26.3 {
        /*return 0;
        *///?} else
        return event.scancode();
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double x, double y, int button) {
        return pointerDown(x, y, button) || super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        return pointerUp(x, y, button) || super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return pointerMove(x, y) || super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return keyDown(key, scancode) || super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scancode, int modifiers) {
        return keyUp(key, scancode) || super.keyReleased(key, scancode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return typed(c) || super.charTyped(c, modifiers);
    }
    *///?}

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        return wheel(x, y, dx, dy) || super.mouseScrolled(x, y, dx, dy);
    }
}
