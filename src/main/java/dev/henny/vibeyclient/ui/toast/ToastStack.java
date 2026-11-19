package dev.henny.vibeyclient.ui.toast;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.ui.Anim;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The toast stack (implements {@link Notify.Sink}): top right (below any vanilla toasts), newest at the top, at most {@link #MAX_VISIBLE} at a
 * time (the rest wait), 150 ms slide in/out, auto-hide (paused while hovered), click = action, optional buttons.
 * Drawn over every screen ({@link ToastHooks}) and on the HUD in game (hidden with F1). Client thread only, except
 * {@link #show} which may be called from anywhere.
 */
public final class ToastStack {
    private ToastStack() {}

    public static final int MAX_VISIBLE = 4;
    public static final int WIDTH = 210;
    private static final int MARGIN = 6;
    private static final int GAP = 4;
    private static final int DEFAULT_MS = 5_000;
    private static final int ACTION_MS = 12_000;

    /** A button on a toast. */
    public record Button(String label, boolean primary, Runnable action) {}

    private static final class Entry {
        final Notify.Toast toast;
        final List<Button> buttons;
        final int durationMs;
        final String key;
        final Anim slide = new Anim(0f);
        final Anim y = new Anim(0f, 150);
        final Anim hover = new Anim(0f);
        boolean placed;
        long shownAt;
        long elapsed;
        long lastFrame;
        boolean leaving;
        String[] lines;
        String[] titleLines = new String[0];
        int linesWidth = -1;
        int hitX, hitY, hitH;

        Entry(Notify.Toast toast, List<Button> buttons, int durationMs, String key) {
            this.toast = toast;
            this.buttons = buttons;
            this.durationMs = durationMs;
            this.key = key;
        }

        int height(boolean hud) {
            int h = 24 + (lines == null ? 0 : lines.length * 10) + Math.max(0, titleLines.length - 1) * 10;
            if (!buttons.isEmpty()) h += 18;
            else if (hud && toast.action() != null) h += 9; // "Open with G" row
            return Math.max(30, h);
        }
    }

    private static final List<Entry> visible = new ArrayList<>();
    private static final List<Entry> waiting = new ArrayList<>();
    private static final List<Entry> incoming = new ArrayList<>();
    private static final Object LOCK = new Object();

    private static int lastX = Integer.MIN_VALUE;
    private static java.util.function.Supplier<String> hint = () -> Lang.tr("toast.openHint");

    /** Lay the stack out below the vanilla toasts (tests switch it off for a comparison shot). */
    public static volatile boolean belowVanilla = true;
    private static int lastTop = -1;

    private static int reserveRight;
    private static long reserveUntil;

    /** Keeps the stack left of a panel on the right edge ({@code px} wide) for this frame (e.g. the social sidebar). */
    public static void reserveRight(int px) {
        reserveRight = px;
        reserveUntil = System.currentTimeMillis() + 100;
    }

    /** Text shown on clickable toasts in game, where the mouse is captured (e.g. "Open with G"). */
    public static void setHint(java.util.function.Supplier<String> h) {
        hint = h;
    }

    /** {@link Notify.Sink}: shows a toast (any thread). */
    public static void show(Notify.Toast toast) {
        show(toast, List.of(), toast.action() != null ? 7_000 : DEFAULT_MS, null);
    }

    /**
     * A toast with buttons. {@code key}: replaces a visible/waiting toast with the same key (e.g. one per chat), may
     * be null. {@code durationMs <= 0}: default.
     */
    public static void show(Notify.Toast toast, List<Button> buttons, int durationMs, String key) {
        int ms = durationMs > 0 ? durationMs : buttons.isEmpty() ? DEFAULT_MS : ACTION_MS;
        synchronized (LOCK) {
            incoming.add(new Entry(toast, List.copyOf(buttons), ms, key));
        }
    }

    /** Removes toasts with {@code key} (e.g. the invite was answered elsewhere). */
    public static void dismiss(String key) {
        if (key == null) return;
        synchronized (LOCK) {
            incoming.removeIf(e -> key.equals(e.key));
        }
        waiting.removeIf(e -> key.equals(e.key));
        for (Entry e : visible) if (key.equals(e.key)) e.leaving = true;
    }

    /**
     * Screen position of button {@code index} of the newest visible toast whose key starts with {@code keyPrefix}, as
     * last drawn (for tests: feed it to {@link #click}); null if there is none.
     */
    public static int[] buttonCenter(String keyPrefix, int index) {
        if (lastX == Integer.MIN_VALUE) return null;
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        for (Entry e : visible) {
            if (e.leaving || e.key == null || !e.key.startsWith(keyPrefix) || index < 0 || index >= e.buttons.size()) continue;
            int w = e.linesWidth > 0 ? e.linesWidth : WIDTH;
            int[] widths = buttonWidths(font, e, w);
            int bx = e.hitX + 34;
            for (int i = 0; i < index; i++) bx += widths[i] + 4;
            return new int[]{bx + widths[index] / 2, e.hitY + e.hitH - 18 + 6};
        }
        return null;
    }

    /** Target y of the topmost toast in the last frame (tests), -1 before the first frame with toasts. */
    public static int debugTop() {
        return lastTop;
    }

    /** Keys of the visible toasts (tests/diagnostics). */
    public static String debugKeys() {
        List<String> keys = new ArrayList<>();
        for (Entry e : visible) if (!e.leaving) keys.add(String.valueOf(e.key));
        return String.join(",", keys);
    }

    public static boolean isEmpty() {
        synchronized (LOCK) {
            if (!incoming.isEmpty()) return false;
        }
        return visible.isEmpty() && waiting.isEmpty();
    }

    // ============================================================================================ frame

    private static void pump() {
        synchronized (LOCK) {
            for (Entry e : incoming) {
                if (e.key != null) {
                    waiting.removeIf(w -> e.key.equals(w.key));
                    for (Entry v : visible) if (e.key.equals(v.key)) v.leaving = true;
                }
                waiting.add(e);
            }
            incoming.clear();
        }
        int active = 0;
        for (Entry e : visible) if (!e.leaving) active++;
        while (active < MAX_VISIBLE && !waiting.isEmpty()) {
            Entry e = waiting.remove(0);
            e.shownAt = System.currentTimeMillis();
            e.lastFrame = e.shownAt;
            e.slide.snap(0f);
            visible.add(0, e); // newest on top
            active++;
        }
    }

    /**
     * Draws the stack. {@code mouseX/Y} = -1 when the mouse is not usable (HUD in game).
     */
    public static void render(Gfx g, int mouseX, int mouseY) {
        pump();
        if (visible.isEmpty()) return;
        long now = System.currentTimeMillis();
        int sw = g.screenWidth();
        int reserve = now < reserveUntil ? reserveRight : 0;
        // beside a panel on the right if there is room for a readable toast, else over it
        if (sw - reserve - MARGIN * 2 < 120) reserve = 0;
        int w = Math.min(WIDTH, sw - reserve - MARGIN * 2);
        int x = sw - MARGIN - w - reserve;
        lastX = x;
        // below the vanilla toasts (advancements, recipes, "chat messages can't be verified"): they share the corner
        int y = MARGIN + (belowVanilla ? vanillaOffset() : 0);
        lastTop = y;
        for (int i = 0; i < visible.size(); i++) {
            Entry e = visible.get(i);
            layout(g, e, w);
            int h = e.height(mouseX < 0);
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
            // timer (paused while hovered)
            long dt = now - e.lastFrame;
            e.lastFrame = now;
            if (!hovered && !e.leaving) e.elapsed += dt;
            if (e.elapsed >= e.durationMs) e.leaving = true;
            float s = e.slide.to(e.leaving ? 0f : 1f);
            if (e.leaving && s <= 0.001f) {
                visible.remove(i);
                i--;
                continue;
            }
            if (!e.placed) {
                e.y.snap(y);
                e.placed = true;
            }
            int ty = Math.round(e.y.to(y));
            int off = Math.round((1f - s) * (w + MARGIN + 4));
            drawEntry(g, e, x + off, ty, w, h, hovered && !e.leaving, mouseX, mouseY, now);
            e.hitX = x + off;
            e.hitY = ty;
            e.hitH = h;
            y += h + GAP;
        }
    }

    /** Room taken by vanilla toasts at the top right (0 if none; follows their slide-out). */
    private static int vanillaOffset() {
        float bottom;
        try {
            bottom = dev.henny.vibeyclient.client.ClientCompat.vanillaToastBottom();
        } catch (Throwable t) {
            bottom = 0f; // accessor missing (another mod replaced the toast manager)
        }
        return bottom <= 0.5f ? 0 : Math.round(bottom) - MARGIN + GAP;
    }

    private static void layout(Gfx g, Entry e, int w) {
        if (e.linesWidth == w) return;
        e.linesWidth = w;
        e.titleLines = TextWrap.wrap(g, e.toast.title(), w - 50, 2).toArray(new String[0]);
        String body = e.toast.body();
        if (body == null || body.isBlank()) {
            e.lines = new String[0];
            return;
        }
        List<String> out = TextWrap.wrap(g, body, w - 44, 2);
        e.lines = out.toArray(new String[0]);
    }

    private static int kindColor(Notify.Kind k) {
        return switch (k) {
            case SUCCESS -> Theme.OK;
            case WARN -> Theme.WARN;
            case ERROR -> Theme.DANGER;
            case SOCIAL -> Theme.BRAND;
            default -> Theme.BRAND2;
        };
    }

    private static void drawEntry(Gfx g, Entry e, int x, int y, int w, int h, boolean hovered, int mx, int my, long now) {
        float hov = e.hover.to(hovered ? 1f : 0f);
        int accent = kindColor(e.toast.kind());
        g.rect(x, y, w, h, Theme.mix(Theme.SURFACE, Theme.SURFACE2, hov));
        g.outline(x, y, w, h, Theme.mix(Theme.LINE2, Theme.alpha(accent, 0.6f), hov));
        g.rect(x, y, 2, h, accent);
        // icon
        int ix = x + 9;
        int iy = y + 8;
        String icon = e.toast.iconKey();
        if (icon != null && looksLikeUuid(icon)) {
            Heads.draw(g, icon, null, ix, iy, 18);
            g.outline(ix - 1, iy - 1, 20, 20, Theme.LINE2);
        } else if (icon != null && !icon.isEmpty() && !icon.equals("!")) {
            Widgets.monogram(g, icon, ix, iy, 18, accent);
        } else {
            g.rect(ix, iy, 18, 18, Theme.alpha(accent, 0.18f));
            g.outline(ix, iy, 18, 18, Theme.alpha(accent, 0.6f));
            glyph(g, e.toast.kind(), ix, iy, accent);
        }
        int tx = x + 34;
        int tw = w - 34 - 16;
        int ly = y + 7;
        for (String t : e.titleLines) {
            g.text(t, tx, ly, Theme.TEXT);
            ly += 10;
        }
        ly += 1;
        if (e.lines != null) {
            for (String line : e.lines) {
                g.text(line, tx, ly, Theme.MUTED);
                ly += 10;
            }
        }
        // close cross (on hover)
        if (hovered) {
            boolean ch = mx >= x + w - 14 && mx < x + w - 2 && my >= y + 2 && my < y + 14;
            Widgets.cross(g, x + w - 11, y + 5, 6, ch ? Theme.TEXT : Theme.DIM);
        }
        // buttons
        if (!e.buttons.isEmpty() && mx < 0) {
            // In game the mouse is captured: the buttons can't be clicked, say how to answer instead.
            g.text(g.clip(hint.get(), tw), tx, y + h - 14, Theme.BRAND2);
        } else if (!e.buttons.isEmpty()) {
            int by = y + h - 18;
            int bx = tx;
            int[] widths = buttonWidths(g.font, e, w);
            for (int bi = 0; bi < e.buttons.size(); bi++) {
                Button b = e.buttons.get(bi);
                int bw = widths[bi];
                boolean bh = mx >= bx && mx < bx + bw && my >= by && my < by + 13;
                int bg = b.primary() ? (bh ? Theme.BRAND_HOVER : Theme.BRAND) : (bh ? Theme.SURFACE3 : Theme.SURFACE2);
                g.rect(bx, by, bw, 13, bg);
                if (!b.primary()) g.outline(bx, by, bw, 13, Theme.LINE2);
                g.textCenter(g.clip(b.label(), bw - 4), bx + bw / 2, by + 3, b.primary() ? 0xFFFFFFFF : Theme.TEXT);
                bx += bw + 4;
            }
        } else if (e.toast.action() != null && mx < 0) {
            // In game the mouse is captured: say how to act on it.
            g.textRight(g.clip(hint.get(), tw), x + w - 6, y + h - 10, Theme.DIM);
        }
        // remaining time
        float left = 1f - Math.min(1f, e.elapsed / (float) e.durationMs);
        g.rect(x + 2, y + h - 1, Math.round((w - 2) * left), 1, Theme.alpha(accent, 0.5f));
    }

    /** Button widths that fit the toast ({@code w} = toast width; buttons start at +34, 6 px right margin). */
    private static int[] buttonWidths(net.minecraft.client.gui.Font font, Entry e, int w) {
        int n = e.buttons.size();
        int[] out = new int[n];
        int avail = w - 34 - 6 - (n - 1) * 4;
        int total = 0;
        for (int i = 0; i < n; i++) {
            out[i] = Math.max(44, font.width(e.buttons.get(i).label()) + 14);
            total += out[i];
        }
        if (total > avail) for (int i = 0; i < n; i++) out[i] = Math.max(20, avail / n);
        return out;
    }

    /** Small pixel glyph: check (success), ! (warn/error/social), i (info). */
    private static void glyph(Gfx g, Notify.Kind kind, int x, int y, int c) {
        switch (kind) {
            case SUCCESS -> {
                for (int i = 0; i < 3; i++) g.rect(x + 5 + i, y + 9 + i, 2, 2, c);
                for (int i = 0; i < 6; i++) g.rect(x + 8 + i, y + 10 - i, 2, 2, c);
            }
            case INFO -> {
                g.rect(x + 8, y + 5, 2, 2, c);
                g.rect(x + 8, y + 8, 2, 6, c);
            }
            default -> {
                g.rect(x + 8, y + 4, 2, 7, c);
                g.rect(x + 8, y + 12, 2, 2, c);
            }
        }
    }

    private static boolean looksLikeUuid(String s) {
        if (s.length() != 36) return false;
        try {
            UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ============================================================================================ input

    /** A click on a screen; true if a toast used it. Client thread. */
    public static boolean click(double mx, double my, int button) {
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        if (visible.isEmpty() || lastX == Integer.MIN_VALUE) return false;
        for (Entry e : visible) {
            if (e.leaving) continue;
            int x = e.hitX;
            int y = e.hitY;
            int h = e.hitH;
            int w = e.linesWidth > 0 ? e.linesWidth : WIDTH;
            if (mx < x || mx >= x + w || my < y || my >= y + h) continue;
            if (button != 0) {
                e.leaving = true;
                return true;
            }
            if (mx >= x + w - 14 && my < y + 14) {
                e.leaving = true;
                return true;
            }
            if (!e.buttons.isEmpty()) {
                int by = y + h - 18;
                int bx = x + 34;
                int[] widths = buttonWidths(font, e, w);
                for (int bi = 0; bi < e.buttons.size(); bi++) {
                    Button b = e.buttons.get(bi);
                    int bw = widths[bi];
                    if (mx >= bx && mx < bx + bw && my >= by && my < by + 13) {
                        e.leaving = true;
                        run(b.action());
                        return true;
                    }
                    bx += bw + 4;
                }
            }
            e.leaving = true;
            run(e.toast.action());
            return true;
        }
        return false;
    }

    private static void run(Runnable r) {
        if (r == null) return;
        try {
            r.run();
        } catch (Throwable t) {
            dev.henny.vibeyclient.Vibey.LOG.error("Toast action failed", t);
        }
    }
}
