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
    // which rects take keyboard focus
    private boolean[] focusable = new boolean[64];
    private boolean[] lastFocusable = new boolean[64];

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

    // Keyboard focus (Tab / Shift+Tab, Enter/Space activates): index into the clickable rects of the frame.
    private int focus = -1;
    private boolean activate;
    private boolean focusOwned;

    // Tooltip requested this frame (drawn on top in end()), shown after a short hover delay.
    private Object tipKey;
    private String tipText;
    private Object shownTipKey;
    private long tipSince;

    public Ui(Consumer<EditBox> addWidget) {
        this.addWidget = addWidget;
    }

    // ============================================================================================ frame

    public void begin(Gfx g, int mouseX, int mouseY, float delta) {
        this.g = g;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.delta = delta;
        rectCount = 0;
        tipKey = null;
        tipText = null;
        focusOwned = false;
        resetClip();
        for (Input in : inputs.values()) in.seen = false;
    }

    public void end() {
        drawFocus();
        drawTooltip();
        pendingClick = false;
        activate = false;
        pendingScroll = 0;
        int[] t = lastRects;
        lastRects = rects;
        rects = t;
        boolean[] f = lastFocusable;
        lastFocusable = focusable;
        focusable = f;
        lastRectCount = rectCount;
        for (Input in : inputs.values()) {
            if (!in.seen) {
                in.box.visible = false;
                if (in.box.isFocused()) in.box.setFocused(false);
            }
        }
    }

    /** Re-adds the text fields after the screen was re-initialised (resize). */
    public void reattach() {
        for (Input in : inputs.values()) addWidget.accept(in.box);
    }

    // ============================================================================================ input from the screen

    public boolean queueClick(double x, double y, int button) {
        focus = -1;
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

    public void release() {
        mouseDown = false;
        dragging = null;
    }

    public void queueScroll(double amount) {
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
        return register(x, y, w, h, button, true);
    }

    /** Like {@link #clicked(int, int, int, int)} but never gets the keyboard focus (decoration, e.g. the seal). */
    public boolean clickedNoFocus(int x, int y, int w, int h) {
        return register(x, y, w, h, 0, false);
    }

    private boolean register(int x, int y, int w, int h, int button, boolean canFocus) {
        int x1 = Math.max(x, clipX1), y1 = Math.max(y, clipY1);
        int x2 = Math.min(x + w, clipX2), y2 = Math.min(y + h, clipY2);
        if (x2 <= x1 || y2 <= y1) return false;
        if (rectCount * 4 + 4 > rects.length) rects = java.util.Arrays.copyOf(rects, rects.length * 2);
        if (rectCount + 1 > focusable.length) focusable = java.util.Arrays.copyOf(focusable, focusable.length * 2);
        focusable[rectCount] = canFocus;
        int o = rectCount++ * 4;
        rects[o] = x1;
        rects[o + 1] = y1;
        rects[o + 2] = x2 - x1;
        rects[o + 3] = y2 - y1;
        if (activate && button == 0 && rectCount - 1 == focus) {
            activate = false;
            capturing = null;
            return true;
        }
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
        if (register(x, y, w, h, 0, false)) dragging = key;
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

    public void stopCapture() {
        capturing = null;
    }

    // ============================================================================================ keyboard focus

    /**
     * Keyboard navigation over everything clickable: Tab / Shift+Tab (or the arrow keys when {@code arrows}) move
     * the focus ring, Enter/Space click the focused control. Returns true if the key was used. The screen calls this
     * before panels see the key.
     */
    public boolean key(int keyCode, boolean shift, boolean arrows) {
        int n = lastRectCount;
        if (n == 0) return false;
        int dir = 0;
        if (keyCode == dev.henny.vibeyclient.client.Keys.TAB) dir = shift ? -1 : 1;
        else if (arrows && keyCode == dev.henny.vibeyclient.client.Keys.DOWN) dir = 1;
        else if (arrows && keyCode == dev.henny.vibeyclient.client.Keys.UP) dir = -1;
        if (dir != 0) {
            for (Input in : inputs.values()) in.box.setFocused(false);
            int f = focus < 0 ? (dir > 0 ? -1 : n) : focus;
            for (int i = 0; i < n; i++) {
                f = Math.floorMod(f + dir, n);
                if (f < lastFocusable.length && lastFocusable[f]) {
                    focus = f;
                    return true;
                }
            }
            return false;
        }
        if ((keyCode == dev.henny.vibeyclient.client.Keys.ENTER || keyCode == dev.henny.vibeyclient.client.Keys.SPACE
                || keyCode == 335 /* keypad enter */) && focus >= 0 && focus < n) {
            activate = true;
            return true;
        }
        return false;
    }

    /**
     * True while the keyboard focus is on the clickable area registered last. A widget that asks draws its own
     * focus state (like hover); otherwise the context draws a 1 px brand ring around the focused area.
     */
    public boolean focusedLast() {
        boolean f = focus >= 0 && focus == rectCount - 1;
        if (f) focusOwned = true;
        return f;
    }

    public boolean keyboardMode() {
        return focus >= 0;
    }

    public void clearFocus() {
        focus = -1;
    }

    private void drawFocus() {
        if (focus < 0 || focus >= rectCount || g == null || focusOwned) return;
        int o = focus * 4;
        g.outline(rects[o] - 1, rects[o + 1] - 1, rects[o + 2] + 2, rects[o + 3] + 2, Theme.BRAND);
    }

    // ============================================================================================ tooltips

    /** Shows {@code text} as a tooltip if the mouse stays over x, y, w, h (call every frame, after drawing the control). */
    public void tooltip(Object key, int x, int y, int w, int h, String text) {
        if (text == null || text.isEmpty() || !hover(x, y, w, h)) return;
        tipKey = key;
        tipText = text;
    }

    private void drawTooltip() {
        if (tipKey == null) {
            shownTipKey = null;
            return;
        }
        long now = System.currentTimeMillis();
        if (tipKey != shownTipKey) {
            shownTipKey = tipKey;
            tipSince = now;
        }
        if (now - tipSince < 350) return;
        drawTooltipBox(g, tipText, mouseX, mouseY);
    }

    /** The Vibe tooltip box (also usable by screens that do not use {@link Ui}). Wraps at 200 px. */
    public static void drawTooltipBox(Gfx g, String text, int mx, int my) {
        java.util.List<String> lines = wrap(g, text, 200);
        int w = 0;
        for (String l : lines) w = Math.max(w, g.width(l));
        w += 10;
        int h = lines.size() * 10 + 7;
        int x = mx + 10, y = my + 12;
        if (x + w > g.screenWidth() - 2) x = Math.max(2, mx - w - 6);
        if (y + h > g.screenHeight() - 2) y = Math.max(2, my - h - 6);
        g.layer();
        g.rect(x, y, w, h, 0xF2101014);
        g.outline(x, y, w, h, Theme.LINE2);
        int ty = y + 4;
        for (String l : lines) {
            g.text(l, x + 5, ty, Theme.TEXT);
            ty += 10;
        }
    }

    /** Greedy word wrap with the Minecraft font. */
    public static java.util.List<String> wrap(Gfx g, String text, int maxWidth) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String para : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : para.split(" ")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (g.width(candidate) <= maxWidth || line.length() == 0) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                }
            }
            out.add(line.toString());
        }
        return out;
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

    /** Takes the keyboard focus away from every text field. */
    public void blurInputs() {
        for (Input in : inputs.values()) in.box.setFocused(false);
    }

    public boolean anyInputFocused() {
        for (Input in : inputs.values()) if (in.box.isFocused() && in.box.visible) return true;
        return false;
    }
}
