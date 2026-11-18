package dev.henny.vibeyclient.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Immediate-mode UI context of one {@link VibeyScreen}: every frame the panels lay everything out again and ask
 * {@code if (ui.clicked(x, y, w, h)) ...}. A click is queued by the screen and consumed by the first widget drawn
 * under it in the next frame (so check inner controls before the row around them). Nothing here allocates per
 * frame; per-widget state ({@link Anim}s, text fields) is keyed by a stable object (the setting, the module, ...).
 */
public final class Ui {
    public Gfx g;
    public int mouseX;
    public int mouseY;
    public float delta;

    private boolean pendingClick;
    private double clickX;
    private double clickY;
    private int clickButton;
    private boolean mouseDown;
    private Object dragging;
    private double pendingScroll;
    private Object capturing;

    private int clipX1 = Integer.MIN_VALUE, clipY1 = Integer.MIN_VALUE, clipX2 = Integer.MAX_VALUE, clipY2 = Integer.MAX_VALUE;

    // Clickable rectangles of the last frame (x, y, w, h), to answer "is there something under the mouse" right away.
    private int[] rects = new int[256];
    private int rectCount;
    private int[] lastRects = new int[256];
    private int lastRectCount;

    private final Map<Object, Anim[]> anims = new IdentityHashMap<>();
    private final Map<Object, Input> inputs = new IdentityHashMap<>();
    private final Consumer<EditBox> addWidget;

    private static final class Input {
        final EditBox box;
        Consumer<String> onChange;
        boolean seen;

        Input(EditBox box) {
            this.box = box;
        }
    }

    Ui(Consumer<EditBox> addWidget) {
        this.addWidget = addWidget;
    }

    // ============================================================================================ frame

    void begin(Gfx g, int mouseX, int mouseY, float delta) {
        this.g = g;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.delta = delta;
        rectCount = 0;
        resetClip();
        for (Input in : inputs.values()) in.seen = false;
    }

    void end() {
        pendingClick = false;
        pendingScroll = 0;
        int[] t = lastRects;
        lastRects = rects;
        rects = t;
        lastRectCount = rectCount;
        for (Input in : inputs.values()) {
            if (!in.seen) {
                in.box.visible = false;
                if (in.box.isFocused()) in.box.setFocused(false);
            }
        }
    }

    /** Re-adds the text fields after the screen was re-initialised (resize). */
    void reattach() {
        for (Input in : inputs.values()) addWidget.accept(in.box);
    }

    // ============================================================================================ input from the screen

    boolean queueClick(double x, double y, int button) {
        for (Input in : inputs.values()) {
            EditBox b = in.box;
            if (b.visible && x >= b.getX() - 4 && x < b.getX() + b.getWidth() + 4 && y >= b.getY() - 4 && y < b.getY() + b.getHeight() + 4) {
                return false; // the text field handles it
            }
        }
        for (Input in : inputs.values()) in.box.setFocused(false);
        mouseDown = button == 0;
        boolean hit = false;
        for (int i = 0; i < lastRectCount; i++) {
            int o = i * 4;
            if (x >= lastRects[o] && x < lastRects[o] + lastRects[o + 2] && y >= lastRects[o + 1] && y < lastRects[o + 1] + lastRects[o + 3]) {
                hit = true;
                break;
            }
        }
        if (capturing != null) hit = true; // a click while waiting for a key cancels the capture
        pendingClick = true;
        clickX = x;
        clickY = y;
        clickButton = button;
        return hit;
    }

    void release() {
        mouseDown = false;
        dragging = null;
    }

    void queueScroll(double amount) {
        pendingScroll += amount;
    }

    // ============================================================================================ queries for widgets

    public boolean hover(int x, int y, int w, int h) {
        if (mouseX < clipX1 || mouseX >= clipX2 || mouseY < clipY1 || mouseY >= clipY2) return false;
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /** Registers a clickable area; true once when it was clicked with the left button. */
    public boolean clicked(int x, int y, int w, int h) {
        return clicked(x, y, w, h, 0);
    }

    public boolean clicked(int x, int y, int w, int h, int button) {
        int x1 = Math.max(x, clipX1), y1 = Math.max(y, clipY1);
        int x2 = Math.min(x + w, clipX2), y2 = Math.min(y + h, clipY2);
        if (x2 <= x1 || y2 <= y1) return false;
        if (rectCount * 4 + 4 > rects.length) rects = java.util.Arrays.copyOf(rects, rects.length * 2);
        int o = rectCount++ * 4;
        rects[o] = x1;
        rects[o + 1] = y1;
        rects[o + 2] = x2 - x1;
        rects[o + 3] = y2 - y1;
        if (!pendingClick || clickButton != button) return false;
        if (clickX >= x1 && clickX < x2 && clickY >= y1 && clickY < y2) {
            pendingClick = false;
            capturing = null;
            return true;
        }
        return false;
    }

    /** Starts/continues a drag of {@code key} when the area is pressed; true while dragging it. */
    public boolean dragging(Object key, int x, int y, int w, int h) {
        if (clicked(x, y, w, h)) dragging = key;
        return dragging == key && mouseDown;
    }

    public boolean isDragging(Object key) {
        return dragging == key && mouseDown;
    }

    /** Takes the pending wheel movement if the mouse is over the area (positive = up). */
    public double takeScroll(int x, int y, int w, int h) {
        if (pendingScroll == 0 || !hover(x, y, w, h)) return 0;
        double s = pendingScroll;
        pendingScroll = 0;
        return s;
    }

    /** The next key press goes to {@code key}'s owner (keybind buttons); see {@link #capturing()}. */
    public void capture(Object key) {
        capturing = key;
    }

    public Object capturing() {
        return capturing;
    }

    void stopCapture() {
        capturing = null;
    }

    // ============================================================================================ clipping

    public void clip(int x1, int y1, int x2, int y2) {
        clipX1 = x1;
        clipY1 = y1;
        clipX2 = x2;
        clipY2 = y2;
        g.scissor(x1, y1, x2, y2);
    }

    public void unclip() {
        g.endScissor();
        resetClip();
    }

    private void resetClip() {
        clipX1 = Integer.MIN_VALUE;
        clipY1 = Integer.MIN_VALUE;
        clipX2 = Integer.MAX_VALUE;
        clipY2 = Integer.MAX_VALUE;
    }

    public boolean visibleY(int y, int h) {
        return y + h > clipY1 && y < clipY2;
    }

    // ============================================================================================ state

    /** The animation for {@code key} (created on first use with {@code initial}). Same as slot 0. */
    public Anim anim(Object key, float initial) {
        return anim(key, 0, initial);
    }

    /**
     * Animation {@code slot} (0..7) of {@code key}, so one object can drive several (the widgets use: button 1,
     * toggle 2, slider 3, text field 4; slot 0 and 5..7 are free for panels).
     */
    public Anim anim(Object key, int slot, float initial) {
        Anim[] a = anims.get(key);
        if (a == null) {
            a = new Anim[8];
            anims.put(key, a);
        }
        if (a[slot] == null) a[slot] = new Anim(initial);
        return a[slot];
    }

    /**
     * A vanilla text field for {@code key}, positioned and drawn by {@link Widgets#textField}. {@code onChange} gets
     * every edit.
     */
    EditBox input(Object key, String value, int maxLength, Consumer<String> onChange) {
        Input in = inputs.get(key);
        if (in == null) {
            EditBox box = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 12, Component.empty());
            box.setBordered(false);
            box.setMaxLength(maxLength);
            box.setTextColor(Theme.TEXT);
            box.setValue(value);
            in = new Input(box);
            Input fin = in;
            box.setResponder(s -> {
                if (fin.onChange != null) fin.onChange.accept(s);
            });
            inputs.put(key, in);
            addWidget.accept(box);
        } else if (!in.box.isFocused() && !in.box.getValue().equals(value)) {
            in.box.setValue(value);
        }
        in.onChange = onChange;
        in.seen = true;
        return in.box;
    }

    boolean anyInputFocused() {
        for (Input in : inputs.values()) if (in.box.isFocused() && in.box.visible) return true;
        return false;
    }
}
