package dev.henny.vibeyclient.module.cosmetics.ui;

import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.ui.Anim;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Widgets;

import java.util.HashMap;
import java.util.Map;

/**
 * Immediate-mode context for the full-screen wardrobe screens (same model as the core {@code Ui} of the Vibey menu,
 * which can only be created by {@code VibeyScreen}): every frame lays everything out again and asks
 * {@code if (ui.clicked(x, y, w, h)) ...}. A click is queued by the screen and consumed by the first area registered
 * under it in the next frame, so check inner controls before the card around them. Left and right button are
 * supported. Nothing allocates per frame; animation state is keyed by stable objects (item ids, enum constants).
 */
final class ScreenUi {
    /** Height of the header bar of the wardrobe screens. */
    static final int HEADER = 28;

    Gfx g;
    int mouseX;
    int mouseY;
    float delta;

    private boolean pendingClick;
    private double clickX;
    private double clickY;
    private int clickButton;
    private double pendingScroll;

    private int clipX1 = Integer.MIN_VALUE, clipY1 = Integer.MIN_VALUE, clipX2 = Integer.MAX_VALUE, clipY2 = Integer.MAX_VALUE;

    private int[] rects = new int[256];
    private int rectCount;
    private int[] lastRects = new int[256];
    private int lastRectCount;

    private final Map<Object, Anim[]> anims = new HashMap<>();

    // ============================================================================================ frame

    void begin(Gfx g, int mouseX, int mouseY, float delta) {
        this.g = g;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.delta = delta;
        rectCount = 0;
        resetClip();
    }

    void end() {
        pendingClick = false;
        pendingScroll = 0;
        int[] t = lastRects;
        lastRects = rects;
        rects = t;
        lastRectCount = rectCount;
    }

    // ============================================================================================ input from the screen

    /** Queues a click for the next frame; true if a clickable area of the last frame is under it. */
    boolean queueClick(double x, double y, int button) {
        boolean hit = false;
        for (int i = 0; i < lastRectCount; i++) {
            int o = i * 4;
            if (x >= lastRects[o] && x < lastRects[o] + lastRects[o + 2] && y >= lastRects[o + 1] && y < lastRects[o + 1] + lastRects[o + 3]) {
                hit = true;
                break;
            }
        }
        pendingClick = true;
        clickX = x;
        clickY = y;
        clickButton = button;
        return hit;
    }

    void queueScroll(double amount) {
        pendingScroll += amount;
    }

    // ============================================================================================ queries

    boolean hover(int x, int y, int w, int h) {
        if (mouseX < clipX1 || mouseX >= clipX2 || mouseY < clipY1 || mouseY >= clipY2) return false;
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    boolean clicked(int x, int y, int w, int h) {
        return clicked(x, y, w, h, 0);
    }

    /** Registers a clickable area; true once when it was clicked with {@code button} (0 left, 1 right). */
    boolean clicked(int x, int y, int w, int h, int button) {
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
            return true;
        }
        return false;
    }

    /** Takes the pending wheel movement if the mouse is over the area (positive = up). */
    double takeScroll(int x, int y, int w, int h) {
        if (pendingScroll == 0 || !hover(x, y, w, h)) return 0;
        double s = pendingScroll;
        pendingScroll = 0;
        return s;
    }

    // ============================================================================================ clipping

    void clip(int x1, int y1, int x2, int y2) {
        clipX1 = x1;
        clipY1 = y1;
        clipX2 = x2;
        clipY2 = y2;
        g.scissor(x1, y1, x2, y2);
    }

    void unclip() {
        g.endScissor();
        resetClip();
    }

    private void resetClip() {
        clipX1 = Integer.MIN_VALUE;
        clipY1 = Integer.MIN_VALUE;
        clipX2 = Integer.MAX_VALUE;
        clipY2 = Integer.MAX_VALUE;
    }

    boolean visibleY(int y, int h) {
        return y + h > clipY1 && y < clipY2;
    }

    // ============================================================================================ state

    /** Animation {@code slot} (0..7) of {@code key}; keys are compared with equals (item id strings work). */
    Anim anim(Object key, int slot, float initial) {
        Anim[] a = anims.get(key);
        if (a == null) {
            a = new Anim[8];
            anims.put(key, a);
        }
        if (a[slot] == null) a[slot] = new Anim(initial);
        return a[slot];
    }

    // ============================================================================================ widgets

    /** Flat square button in the look of {@link Widgets#button}. True when clicked. */
    boolean button(Object key, int x, int y, int w, int h, String label, Widgets.Style style) {
        boolean enabled = style != Widgets.Style.DISABLED;
        boolean clicked = enabled && clicked(x, y, w, h);
        float hov = anim(key, 1, 0f).to(enabled && hover(x, y, w, h) ? 1f : 0f);
        int bg, fg, line;
        switch (style) {
            case PRIMARY -> {
                bg = Theme.mix(Theme.BRAND, Theme.BRAND_HOVER, hov);
                fg = 0xFFFFFFFF;
                line = 0;
            }
            case DANGER -> {
                bg = Theme.mix(Theme.SURFACE2, 0xFF2A1218, hov);
                fg = Theme.mix(Theme.TEXT, Theme.DANGER, hov);
                line = Theme.mix(Theme.LINE2, Theme.DANGER, hov);
            }
            case GHOST -> {
                bg = Theme.alpha(Theme.SURFACE3, hov);
                fg = Theme.mix(Theme.MUTED, Theme.TEXT, hov);
                line = 0;
            }
            case DISABLED -> {
                bg = Theme.SURFACE;
                fg = Theme.DIM;
                line = Theme.LINE;
            }
            default -> {
                bg = Theme.mix(Theme.SURFACE2, Theme.SURFACE3, hov);
                fg = Theme.TEXT;
                line = Theme.mix(Theme.LINE2, 0x40FFFFFF, hov);
            }
        }
        g.rect(x, y, w, h, bg);
        if (line != 0) g.outline(x, y, w, h, line);
        g.textCenter(g.clip(label, w - 8), x + w / 2, y + (h - 8) / 2, fg);
        return clicked;
    }

    /** The square close button of the header (X). True when clicked. */
    boolean closeButton(Object key, int x, int y) {
        boolean clicked = clicked(x, y, 18, 18);
        float hov = anim(key, 2, 0f).to(hover(x, y, 18, 18) ? 1f : 0f);
        if (hov > 0f) g.rect(x, y, 18, 18, Theme.alpha(Theme.SURFACE3, hov));
        Widgets.cross(g, x + 5, y + 5, 8, Theme.mix(Theme.MUTED, Theme.TEXT, hov));
        return clicked;
    }

    /** Header bar of the wardrobe screens: seal icon, bold title, a dim subtitle. Returns the x after the text. */
    int header(int width, String title, String subtitle) {
        g.rect(0, 0, width, HEADER, Theme.SURFACE);
        g.hline(0, HEADER, width, Theme.LINE);
        int x = Theme.PAD;
        g.image(Compat.id("icon.png"), x, (HEADER - 16) / 2, 16, 16, 512);
        x += 22;
        int ty = (HEADER - 8) / 2;
        g.bold(title, x, ty, Theme.TEXT);
        x += g.boldWidth(title) + 8;
        if (subtitle != null && !subtitle.isEmpty()) {
            g.text(subtitle, x, ty, Theme.DIM);
            x += g.width(subtitle) + 8;
        }
        return x;
    }
}
