package dev.henny.vibeyclient.ui.hud;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.Anim;
import dev.henny.vibeyclient.ui.BaseScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.UiHost;
import dev.henny.vibeyclient.ui.Widgets;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * The HUD editor: the game keeps running behind it (slightly dimmed) and every HUD element is drawn where it sits.
 *
 * <ul>
 *   <li>Drag to move (snaps to the screen edges, the centre and other elements, with guides and distance readouts;
 *       hold Ctrl or Alt to place freely). Marquee-drag on empty space or Ctrl/Shift-click to select several; they
 *       move together.</li>
 *   <li>Eight handles resize: proportional by default, Shift = free (widgets: the box size). Mouse wheel scales.</li>
 *   <li>Arrow keys nudge 1 px (Shift 10), Ctrl+Z / Ctrl+Y / Ctrl+Shift+Z undo and redo, Ctrl+A selects all,
 *       Ctrl+D duplicates, Delete/H hides, R resets.</li>
 *   <li>Right-click: Style…, Duplicate, Hide, Reset. The dock at the bottom opens the widget library, templates and
 *       share codes; Done (Esc) saves, Cancel restores the layout from when the editor opened.</li>
 * </ul>
 * Nothing on the canvas path allocates per frame (lists are reused, number labels cached).
 */
public final class HudEditorScreen extends BaseScreen {
    static final int SNAP = 4;
    static final int MARGIN = 4;
    static final int HANDLE = 5;

    /** While open, the elements are only drawn by the editor (the normal HUD skips them). */
    static boolean open;

    final Screen parent;
    private final HudLayout.State before;
    Ui ui;
    final HudEditorPanels panels = new HudEditorPanels(this);

    /** Every registered element / the available ones (library) / the ones on the canvas. Reused every frame. */
    private final List<HudElement> registered = new ArrayList<>();
    final List<HudElement> available = new ArrayList<>();
    final List<HudElement> canvas = new ArrayList<>();
    final List<HudElement> selection = new ArrayList<>();
    HudElement hovered;
    private int hoverHandle = -1;
    /** Element the library row under the mouse points at (pulses on the canvas). */
    HudElement highlight;

    // ------------------------------------------------------------------------------------------- interaction
    private static final int IDLE = 0, PRESS = 1, MOVE = 2, RESIZE = 3, MARQUEE = 4;
    private int mode;
    private double pressX;
    private double pressY;
    private HudElement pressed;
    private boolean pressToggle;
    private boolean pressWasSelected;
    private float[] startX = new float[32];
    private float[] startY = new float[32];
    private final HudLayout.Box group = new HudLayout.Box();
    private final HudLayout.Box groupStart = new HudLayout.Box();
    private int handle;
    private final HudLayout.Box resizeStart = new HudLayout.Box();
    private float resizeScale;
    private int resizeBw;
    private int resizeBh;
    private float mqX1, mqY1, mqX2, mqY2;
    private final List<HudElement> marqueeBase = new ArrayList<>();
    private HudLayout.State dragBefore;
    private String sizeChip;
    private int sizeChipKey = Integer.MIN_VALUE;

    // guides: [vertical?1:0, at, from, to]; distance readouts: [x1, y1, x2, y2, value]
    private final float[] guides = new float[4 * 16];
    private int guideCount;
    private final float[] dists = new float[5 * 8];
    private int distCount;
    private float anchorDotX = Float.NaN;
    private float anchorDotY;

    // ------------------------------------------------------------------------------------------- history
    private final ArrayDeque<HudLayout.State> undo = new ArrayDeque<>();
    private final ArrayDeque<HudLayout.State> redo = new ArrayDeque<>();
    private Object lastKey;
    private long lastKeyAt;
    private boolean saved;

    // ------------------------------------------------------------------------------------------- look
    private final Anim dockShown = new Anim(1f);
    private final Anim dockTop = new Anim(0f);
    private final Anim selectAnim = new Anim(0f);
    private final HudLayout.Box tmp = new HudLayout.Box();
    private final HudLayout.Box tmp2 = new HudLayout.Box();
    private long openedAt;
    int dockX, dockY, dockW, dockH;

    public HudEditorScreen(Screen parent) {
        super(Component.literal(Lang.tr("core.hud.title")));
        this.parent = parent;
        this.before = HudLayout.capture();
        HudIcons.init();
    }

    @Override
    protected void init() {
        open = true;
        if (ui == null) {
            ui = UiHost.create(b -> addWidget(b));
            openedAt = System.currentTimeMillis();
            if (!HudLayout.flag("intro")) panels.openIntro();
        } else {
            UiHost.reattach(ui);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        open = false;
        if (!saved) {
            saved = true;
            HudLayout.save();
        }
    }

    @Override
    public void onClose() {
        done();
    }

    void done() {
        saved = true;
        HudLayout.save();
        open = false;
        ClientCompat.openScreen(parent);
    }

    void cancel() {
        HudLayout.restore(before);
        HudLayout.save();
        saved = true;
        open = false;
        ClientCompat.openScreen(parent);
    }

    // ============================================================================================ history

    /** Records the layout before a change. Changes with the same {@code key} within 800 ms merge into one step. */
    void checkpoint(Object key) {
        long now = System.currentTimeMillis();
        if (key != null && key == lastKey && now - lastKeyAt < 800) {
            lastKeyAt = now;
            return;
        }
        lastKey = key;
        lastKeyAt = now;
        push(HudLayout.capture());
    }

    private void push(HudLayout.State s) {
        undo.push(s);
        while (undo.size() > 100) undo.removeLast();
        redo.clear();
    }

    boolean canUndo() {
        return !undo.isEmpty();
    }

    boolean canRedo() {
        return !redo.isEmpty();
    }

    void undo() {
        if (undo.isEmpty()) return;
        redo.push(HudLayout.capture());
        HudLayout.restore(undo.pop());
        lastKey = null;
        afterRestore();
        panels.toast(Lang.tr("core.hud.toast.undo"));
    }

    void redo() {
        if (redo.isEmpty()) return;
        undo.push(HudLayout.capture());
        HudLayout.restore(redo.pop());
        lastKey = null;
        afterRestore();
        panels.toast(Lang.tr("core.hud.toast.redo"));
    }

    private void afterRestore() {
        selection.removeIf(e -> HudLayout.get(e.id()) != e || HudLayout.hidden(e));
        panels.selectionChanged();
    }

    // ============================================================================================ selection

    HudElement primary() {
        return selection.isEmpty() ? null : selection.get(selection.size() - 1);
    }

    boolean isSelected(HudElement e) {
        for (int i = 0; i < selection.size(); i++) if (selection.get(i) == e) return true;
        return false;
    }

    void selectOnly(HudElement e) {
        selection.clear();
        if (e != null) selection.add(e);
        selectAnim.snap(0f);
        panels.selectionChanged();
    }

    private void toggleSelected(HudElement e) {
        if (isSelected(e)) selection.remove(e);
        else selection.add(e);
        panels.selectionChanged();
    }

    // ============================================================================================ drawing

    @Override
    protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
        UiHost.begin(ui, g, mouseX, mouseY, delta);
        int sw = width;
        int sh = height;
        long now = System.currentTimeMillis();
        boolean inWorld = minecraft != null && minecraft.level != null;
        if (inWorld) {
            g.rect(0, 0, sw, sh, 0x33000000);
        } else {
            panorama(g, delta);
            g.rect(0, 0, sw, sh, 0xB80A0A0E);
        }
        refreshLists();
        boolean busy = mode == MOVE || mode == RESIZE;
        grid(g, sw, sh, busy);

        // hover: nothing under a panel; while dragging the dragged one
        boolean overPanel = panels.covers(mouseX, mouseY) || overDock(mouseX, mouseY);
        hovered = null;
        hoverHandle = -1;
        if (!overPanel && mode == IDLE) {
            HudElement p = primary();
            if (selection.size() == 1 && p != null) hoverHandle = handleAt(p, mouseX, mouseY);
            if (hoverHandle < 0) hovered = elementAt(mouseX, mouseY);
            else hovered = p;
        } else if (mode == MOVE || mode == RESIZE) {
            hovered = primary();
        }

        // the elements (selected ones last = on top)
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < canvas.size(); i++) {
                HudElement e = canvas.get(i);
                if (isSelected(e) != (pass == 1)) continue;
                drawElement(g, e, now, sw, sh);
            }
        }
        g.layer();
        for (int i = 0; i < canvas.size(); i++) frame(g, canvas.get(i), now);
        if (selection.size() > 1) groupFrame(g);
        if (selection.size() == 1 && mode != MOVE) handles(g, primary());
        drawGuides(g, sw, sh);
        if (mode == MARQUEE) {
            int x1 = Math.round(Math.min(mqX1, mqX2)), y1 = Math.round(Math.min(mqY1, mqY2));
            int x2 = Math.round(Math.max(mqX1, mqX2)), y2 = Math.round(Math.max(mqY1, mqY2));
            g.rect(x1, y1, x2 - x1, y2 - y1, Theme.alpha(Theme.BRAND, 0.12f));
            g.outline(x1, y1, x2 - x1, y2 - y1, Theme.alpha(Theme.BRAND, 0.9f));
        }
        if (mode == RESIZE && sizeChip != null) {
            HudElement p = primary();
            if (p != null) {
                HudLayout.Box b = HudLayout.box(p, sw, sh, tmp);
                int cw = g.width(sizeChip) + 8;
                int cx = Math.round(b.x + b.w / 2f) - cw / 2;
                int cy = Math.round(b.y + b.h) + 4;
                if (cy + 12 > sh) cy = Math.round(b.y) - 16;
                chip(g, sizeChip, Math.max(2, Math.min(sw - cw - 2, cx)), cy, Theme.BRAND, 0xFFFFFFFF);
            }
        }
        if (canvas.isEmpty() && !panels.libraryOpen()) {
            String s = Lang.tr("core.hud.empty");
            g.textCenter(s, sw / 2, sh / 2 - 14, Theme.TEXT);
            g.textCenter(Lang.tr("core.hud.emptyHint"), sw / 2, sh / 2, Theme.MUTED);
        }
        cursor(g);
        g.layer();
        dock(g, sw, sh);
        panels.draw(g, mouseX, mouseY, now);
        UiHost.end(ui);
        if (drawPointer) pointer(g, mouseX, mouseY);
    }

    /** Draws a mouse pointer (screenshots and recordings do not contain the system cursor). Tests only. */
    public static boolean drawPointer;

    private static void pointer(Gfx g, int x, int y) {
        g.layer();
        for (int i = 0; i < 10; i++) {
            int w = i < 7 ? i + 1 : 10 - i;
            g.rect(x - 1, y + i - 1, w + 2, 1, 0xFF000000);
            g.rect(x, y + i, w, 1, 0xFFFFFFFF);
        }
        g.rect(x + 3, y + 9, 2, 3, 0xFFFFFFFF);
    }

    private void refreshLists() {
        HudLayout.elements(registered);
        available.clear();
        canvas.clear();
        for (int i = 0; i < registered.size(); i++) {
            HudElement e = registered.get(i);
            if (!e.available()) continue;
            available.add(e);
            if (!HudLayout.hidden(e)) canvas.add(e);
        }
        for (int i = selection.size() - 1; i >= 0; i--) {
            HudElement e = selection.get(i);
            if (!canvas.contains(e) || HudLayout.get(e.id()) != e) selection.remove(i);
        }
    }

    /** Faint thirds and centre lines (stronger while dragging). */
    private static void grid(Gfx g, int sw, int sh, boolean busy) {
        int c = busy ? 0x1CFFFFFF : 0x0EFFFFFF;
        g.rect(sw / 3, 0, 1, sh, c);
        g.rect(sw * 2 / 3, 0, 1, sh, c);
        g.rect(0, sh / 3, sw, 1, c);
        g.rect(0, sh * 2 / 3, sw, 1, c);
        int m = busy ? 0x40FFFFFF : 0x1AFFFFFF;
        for (int y = 0; y < sh; y += 6) g.rect(sw / 2, y, 1, 3, m);
        for (int x = 0; x < sw; x += 6) g.rect(x, sh / 2, 3, 1, m);
    }

    private void drawElement(Gfx g, HudElement e, long now, int sw, int sh) {
        HudLayout.Placement p = HudLayout.placement(e);
        boolean disabled = HudLayout.serverDisabled(e);
        if (e.isWidget()) {
            HudLayout.paintWidget(g, e, p, now, sw, sh, true);
        } else {
            HudLayout.Box b = HudLayout.box(e, sw, sh, tmp);
            g.push();
            g.translate(b.x, b.y);
            g.scale(b.scale, b.scale);
            try {
                e.render(g, e.width(), e.height(), true);
            } catch (RuntimeException ex) {
                // a broken preview must not break the editor
            }
            g.pop();
        }
        if (disabled) {
            HudLayout.Box b = HudLayout.box(e, sw, sh, tmp);
            int x = Math.round(b.x), y = Math.round(b.y), w = Math.max(4, Math.round(b.w)), h = Math.max(4, Math.round(b.h));
            g.rect(x, y, w, h, 0xC8141418);
            for (int ly = y + 2; ly < y + h - 1; ly += 3) g.rect(x + 1, ly, w - 2, 1, 0x12FFFFFF);
        }
    }

    private void frame(Gfx g, HudElement e, long now) {
        HudLayout.Box b = HudLayout.box(e, width, height, tmp);
        int x = (int) Math.floor(b.x);
        int y = (int) Math.floor(b.y);
        int w = Math.max(4, Math.round(b.w));
        int h = Math.max(4, Math.round(b.h));
        boolean sel = isSelected(e);
        boolean hov = e == hovered;
        float hv = ui.anim(e, 0, 0f).to(hov || sel ? 1f : 0f);
        boolean disabled = HudLayout.serverDisabled(e);
        if (e == highlight) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(now / 120.0);
            g.outline(x - 2, y - 2, w + 4, h + 4, Theme.alpha(Theme.BRAND2, 0.4f + 0.6f * pulse));
        }
        if (sel) {
            g.outline(x - 1, y - 1, w + 2, h + 2, Theme.BRAND);
        } else if (hv > 0.01f) {
            g.outline(x - 1, y - 1, w + 2, h + 2, Theme.alpha(Theme.BRAND, 0.25f + 0.55f * hv));
        } else {
            dashed(g, x - 1, y - 1, w + 2, h + 2, 0x30FFFFFF);
        }
        if (disabled) {
            String badge = Lang.tr("core.settings.serverDisabled");
            int bw = g.width(badge) + 16;
            int bx = x + w / 2 - bw / 2;
            int by = h >= 14 ? y + h / 2 - 6 : y + h + 3;
            bx = Math.max(2, Math.min(width - bw - 2, bx));
            g.rect(bx, by, bw, 12, 0xF0201418);
            g.outline(bx, by, bw, 12, Theme.alpha(Theme.WARN, 0.8f));
            Icons.draw(g, HudIcons.LOCK, bx + 3, by + 2, 1, Theme.WARN);
            g.text(badge, bx + 13, by + 2, Theme.WARN);
        }
        // name tag for hover / single selection
        if ((hov || (sel && selection.size() == 1)) && mode != MARQUEE && mode != MOVE && mode != RESIZE) {
            String label = tagLabel(e);
            int tw = g.width(label) + 8;
            int ty = y - 14 >= 0 ? y - 14 : y + h + 3 + 11 <= height ? y + h + 3 : y + 1;
            int tx = Math.max(0, Math.min(width - tw, x - 1));
            int bg = sel ? Theme.BRAND : 0xE60B0B0E;
            g.rect(tx, ty, tw, 11, Theme.alpha(bg, Math.max(0.3f, hv)));
            g.text(label, tx + 4, ty + 2, sel ? 0xFFFFFFFF : Theme.TEXT);
        }
    }

    // cached "Name · 125%" labels
    private final java.util.IdentityHashMap<HudElement, Object[]> tags = new java.util.IdentityHashMap<>();

    private String tagLabel(HudElement e) {
        int pct = Math.round(HudLayout.placement(e).scale * 100);
        String lang = Lang.tr("core.hud.title");
        Object[] c = tags.get(e);
        if (c == null || (int) c[0] != pct || c[2] != lang) {
            c = new Object[] {pct, e.displayName() + "  " + pct + "%", lang};
            tags.put(e, c);
        }
        return (String) c[1];
    }

    private void groupFrame(Gfx g) {
        groupBox(group);
        int x = Math.round(group.x) - 3, y = Math.round(group.y) - 3;
        int w = Math.round(group.w) + 6, h = Math.round(group.h) + 6;
        dashed(g, x, y, w, h, Theme.alpha(Theme.BRAND, 0.9f));
        String n = count(selection.size());
        int cw = g.width(n) + 8;
        g.rect(x, y - 11 >= 0 ? y - 11 : y + h, cw, 11, Theme.BRAND);
        g.text(n, x + 4, (y - 11 >= 0 ? y - 11 : y + h) + 2, 0xFFFFFFFF);
    }

    private String countCache;
    private int countCacheN = -1;
    private String countCacheLang;

    private String count(int n) {
        String lang = Lang.tr("core.hud.selected");
        if (n != countCacheN || lang != countCacheLang) {
            countCacheN = n;
            countCacheLang = lang;
            countCache = Lang.tr("core.hud.selected", n);
        }
        return countCache;
    }

    private void handles(Gfx g, HudElement e) {
        if (e == null) return;
        HudLayout.Box b = HudLayout.box(e, width, height, tmp);
        int x = (int) Math.floor(b.x) - 1, y = (int) Math.floor(b.y) - 1;
        int w = Math.max(4, Math.round(b.w)) + 2, h = Math.max(4, Math.round(b.h)) + 2;
        float appear = selectAnim.to(1f);
        for (int i = 0; i < 8; i++) {
            if (!handleVisible(i, w, h)) continue;
            int hx = handleX(i, x, w), hy = handleY(i, y, h);
            boolean hot = i == hoverHandle || (mode == RESIZE && handle == i);
            int s = hot ? HANDLE + 2 : HANDLE;
            int o = s / 2;
            int cx = hx - o, cy = hy - o;
            g.rect(cx, cy, s, s, Theme.alpha(Theme.BRAND, appear));
            g.rect(cx + 1, cy + 1, s - 2, s - 2, Theme.alpha(hot ? Theme.BRAND2 : 0xFFFFFFFF, appear));
        }
    }

    private static boolean handleVisible(int i, int w, int h) {
        if (i % 2 == 0) return true; // corners
        return (i == 1 || i == 5) ? w >= 22 : h >= 18;
    }

    // handles: 0 TL, 1 T, 2 TR, 3 R, 4 BR, 5 B, 6 BL, 7 L
    private static final int[] HX = {-1, 0, 1, 1, 1, 0, -1, -1};
    private static final int[] HY = {-1, -1, -1, 0, 1, 1, 1, 0};

    private static int handleX(int i, int x, int w) {
        return HX[i] < 0 ? x : HX[i] > 0 ? x + w - 1 : x + w / 2;
    }

    private static int handleY(int i, int y, int h) {
        return HY[i] < 0 ? y : HY[i] > 0 ? y + h - 1 : y + h / 2;
    }

    private int handleAt(HudElement e, double mx, double my) {
        HudLayout.Box b = HudLayout.box(e, width, height, tmp2);
        int x = (int) Math.floor(b.x) - 1, y = (int) Math.floor(b.y) - 1;
        int w = Math.max(4, Math.round(b.w)) + 2, h = Math.max(4, Math.round(b.h)) + 2;
        int r = 4;
        for (int i = 0; i < 8; i += 2) {
            if (Math.abs(mx - handleX(i, x, w)) <= r && Math.abs(my - handleY(i, y, h)) <= r) return i;
        }
        for (int i = 1; i < 8; i += 2) {
            if (!handleVisible(i, w, h)) continue;
            if (Math.abs(mx - handleX(i, x, w)) <= r && Math.abs(my - handleY(i, y, h)) <= r) return i;
        }
        // the whole edge works too (thin widgets have no room for edge handles)
        boolean inY = my > y + 2 && my < y + h - 2, inX = mx > x + 2 && mx < x + w - 2;
        if (inY && Math.abs(mx - (x + w - 1)) <= 1.5) return 3;
        if (inY && Math.abs(mx - x) <= 1.5) return 7;
        if (inX && Math.abs(my - (y + h - 1)) <= 1.5) return 5;
        if (inX && Math.abs(my - y) <= 1.5) return 1;
        return -1;
    }

    private void cursor(Gfx g) {
        int h = mode == RESIZE ? handle : hoverHandle;
        if (h >= 0) {
            g.cursor(h == 1 || h == 5 ? Gfx.CURSOR_RESIZE_NS : h == 3 || h == 7 ? Gfx.CURSOR_RESIZE_EW : Gfx.CURSOR_MOVE);
        } else if (mode == MOVE || (mode == IDLE && hovered != null)) {
            g.cursor(Gfx.CURSOR_MOVE);
        }
    }

    static void dashed(Gfx g, int x, int y, int w, int h, int c) {
        for (int i = 0; i < w; i += 4) {
            int l = Math.min(2, w - i);
            g.rect(x + i, y, l, 1, c);
            g.rect(x + i, y + h - 1, l, 1, c);
        }
        for (int i = 0; i < h; i += 4) {
            int l = Math.min(2, h - i);
            g.rect(x, y + i, 1, l, c);
            g.rect(x + w - 1, y + i, 1, l, c);
        }
    }

    static void chip(Gfx g, String s, int x, int y, int bg, int fg) {
        int w = g.width(s) + 8;
        g.rect(x, y, w, 11, bg);
        g.text(s, x + 4, y + 2, fg);
    }

    // ============================================================================================ guides

    private void drawGuides(Gfx g, int sw, int sh) {
        if (mode != MOVE && mode != RESIZE) return;
        int teal = Theme.BRAND2;
        for (int i = 0; i < guideCount; i++) {
            int o = i * 4;
            boolean vertical = guides[o] > 0.5f;
            int at = Math.round(guides[o + 1]);
            int from = Math.round(guides[o + 2]), to = Math.round(guides[o + 3]);
            if (vertical) g.rect(Math.min(sw - 1, at), from, 1, Math.max(1, to - from), teal);
            else g.rect(from, Math.min(sh - 1, at), Math.max(1, to - from), 1, teal);
        }
        for (int i = 0; i < distCount; i++) {
            int o = i * 5;
            int x1 = Math.round(dists[o]), y1 = Math.round(dists[o + 1]);
            int x2 = Math.round(dists[o + 2]), y2 = Math.round(dists[o + 3]);
            int v = Math.round(dists[o + 4]);
            int c = 0xCCFF5FA8;
            if (y1 == y2) {
                int a = Math.min(x1, x2), b = Math.max(x1, x2);
                if (b - a < 2) continue;
                g.rect(a, y1, b - a, 1, c);
                g.rect(a, y1 - 2, 1, 5, c);
                g.rect(b - 1, y1 - 2, 1, 5, c);
                String s = num(v);
                int tw = g.width(s) + 6;
                int tx = (a + b) / 2 - tw / 2;
                g.rect(tx, y1 - 12, tw, 10, 0xFFFF5FA8);
                g.text(s, tx + 3, y1 - 11, 0xFFFFFFFF);
            } else {
                int a = Math.min(y1, y2), b = Math.max(y1, y2);
                if (b - a < 2) continue;
                g.rect(x1, a, 1, b - a, c);
                g.rect(x1 - 2, a, 5, 1, c);
                g.rect(x1 - 2, b - 1, 5, 1, c);
                String s = num(v);
                int tw = g.width(s) + 6;
                int ty = (a + b) / 2 - 5;
                g.rect(x1 + 3, ty, tw, 10, 0xFFFF5FA8);
                g.text(s, x1 + 6, ty + 1, 0xFFFFFFFF);
            }
        }
        if (!Float.isNaN(anchorDotX)) {
            int ax = Math.round(anchorDotX), ay = Math.round(anchorDotY);
            g.rect(ax - 2, ay - 2, 5, 5, Theme.BRAND2);
            g.rect(ax - 1, ay - 1, 3, 3, 0xFF0B0B0E);
        }
    }

    private static final String[] NUMS = new String[4097];
    private static final String[] PCTS = new String[1001];

    /** "125%" (cached). */
    static String pct(int v) {
        if (v < 0 || v >= PCTS.length) return v + "%";
        String s = PCTS[v];
        if (s == null) PCTS[v] = s = v + "%";
        return s;
    }

    static String num(int v) {
        int a = Math.abs(v);
        if (a >= NUMS.length) return Integer.toString(v);
        String s = NUMS[a];
        if (s == null) NUMS[a] = s = Integer.toString(a);
        return s;
    }

    private void addGuide(boolean vertical, float at, float from, float to) {
        if (guideCount >= 16) return;
        int o = guideCount++ * 4;
        guides[o] = vertical ? 1 : 0;
        guides[o + 1] = at;
        guides[o + 2] = Math.min(from, to);
        guides[o + 3] = Math.max(from, to);
    }

    private void addDist(float x1, float y1, float x2, float y2, float v) {
        if (distCount >= 8 || Math.abs(v) < 1) return;
        int o = distCount++ * 5;
        dists[o] = x1;
        dists[o + 1] = y1;
        dists[o + 2] = x2;
        dists[o + 3] = y2;
        dists[o + 4] = v;
    }

    /** Distance readouts of the box: to the screen edges it is anchored to, and to the nearest neighbours. */
    private void measure(HudLayout.Box b, HudElement anchorOf) {
        distCount = 0;
        int sw = width, sh = height;
        Anchor a = HudLayout.placement(anchorOf).anchor;
        float cy = b.y + b.h / 2f, cx = b.x + b.w / 2f;
        if (a.col == 0) addDist(0, cy, b.x, cy, b.x);
        else if (a.col == 2) addDist(b.x + b.w, cy, sw, cy, sw - b.x - b.w);
        else addDist(sw / 2f, cy, cx, cy, cx - sw / 2f);
        if (a.row == 0) addDist(cx, 0, cx, b.y, b.y);
        else if (a.row == 2) addDist(cx, b.y + b.h, cx, sh, sh - b.y - b.h);
        else addDist(cx, sh / 2f, cx, cy, cy - sh / 2f);
        anchorDotX = a.fx() * sw;
        anchorDotY = a.fy() * sh;
        // nearest neighbour to the left/right (overlapping rows) and above/below (overlapping columns)
        float bestL = 48, bestR = 48, bestU = 48, bestD = 48;
        float lX = 0, rX = 0, uY = 0, dY = 0;
        for (int i = 0; i < canvas.size(); i++) {
            HudElement o = canvas.get(i);
            if (isSelected(o)) continue;
            HudLayout.Box ob = HudLayout.box(o, sw, sh, tmp2);
            boolean rows = ob.y < b.y + b.h && ob.y + ob.h > b.y;
            boolean cols = ob.x < b.x + b.w && ob.x + ob.w > b.x;
            if (rows) {
                float dl = b.x - (ob.x + ob.w);
                if (dl >= 0 && dl < bestL) { bestL = dl; lX = ob.x + ob.w; }
                float dr = ob.x - (b.x + b.w);
                if (dr >= 0 && dr < bestR) { bestR = dr; rX = ob.x; }
            }
            if (cols) {
                float du = b.y - (ob.y + ob.h);
                if (du >= 0 && du < bestU) { bestU = du; uY = ob.y + ob.h; }
                float dd = ob.y - (b.y + b.h);
                if (dd >= 0 && dd < bestD) { bestD = dd; dY = ob.y; }
            }
        }
        if (bestL < 48) addDist(lX, cy + 3, b.x, cy + 3, bestL);
        if (bestR < 48) addDist(b.x + b.w, cy + 3, rX, cy + 3, bestR);
        if (bestU < 48) addDist(cx + 3, uY, cx + 3, b.y, bestU);
        if (bestD < 48) addDist(cx + 3, b.y + b.h, cx + 3, dY, bestD);
    }

    // ============================================================================================ dock

    private boolean overDock(int mx, int my) {
        return mx >= dockX && mx < dockX + dockW && my >= dockY && my < dockY + dockH;
    }

    private void dock(Gfx g, int sw, int sh) {
        String lib = Lang.tr("core.hud.dock.widgets");
        String tpl = Lang.tr("core.hud.dock.templates");
        String share = Lang.tr("core.hud.dock.share");
        String done = Lang.tr("core.hud.done");
        String cancel = Lang.tr("core.hud.cancel");
        int bh = 18, pad = 4, gap = 3;
        int wLib = g.width(lib) + 24, wTpl = g.width(tpl) + 24, wShare = g.width(share) + 24;
        int wCancel = Math.max(46, g.width(cancel) + 14), wDone = Math.max(46, g.width(done) + 14);
        int total = pad + wLib + 7 + 18 + gap + 18 + 7 + wTpl + gap + wShare + 7 + wCancel + gap + wDone + pad;
        dockW = total;
        dockH = bh + pad * 2;
        // bottom, unless the selection sits there; slides away while dragging
        int bottomY = sh - dockH - 8;
        boolean top = false;
        for (int i = 0; i < selection.size(); i++) {
            HudLayout.Box b = HudLayout.box(selection.get(i), sw, sh, tmp2);
            if (b.y + b.h > bottomY - 16 && b.x < (sw + total) / 2f && b.x + b.w > (sw - total) / 2f) top = true;
        }
        float t = dockTop.to(top ? 1f : 0f);
        float shown = dockShown.to(mode == MOVE || mode == RESIZE || mode == MARQUEE ? 0f : 1f);
        int topY = 8;
        int y = Math.round(bottomY + (topY - bottomY) * t);
        int hide = Math.round((1f - shown) * (dockH + 12));
        y += t > 0.5f ? -hide : hide;
        // centred in the free area (right of the library panel when it is open)
        int left = panels.libraryRight();
        int x = Math.max(left + 4, left + (sw - left - total) / 2);
        dockX = x;
        dockY = y;
        if (shown < 0.02f) return;
        g.rect(x, y + 2, total, dockH, 0x50000000);
        g.rect(x, y, total, dockH, 0xF50B0B0E);
        g.outline(x, y, total, dockH, Theme.LINE2);
        g.rect(x, y, total, 1, Theme.alpha(Theme.BRAND, 0.55f));
        int cx = x + pad, cy = y + pad;
        if (dockButton(g, "dock-lib", cx, cy, wLib, bh, HudIcons.LIBRARY, lib, panels.libraryOpen())) panels.toggleLibrary();
        ui.tooltip("dock-lib", cx, cy, wLib, bh, Lang.tr("core.hud.dock.widgets.tip"));
        cx += wLib + 3;
        g.rect(cx, cy + 3, 1, bh - 6, Theme.LINE2);
        cx += 4;
        if (iconButton(g, "dock-undo", cx, cy, HudIcons.UNDO, canUndo())) undo();
        ui.tooltip("dock-undo", cx, cy, 18, bh, Lang.tr("core.hud.dock.undo"));
        cx += 18 + gap;
        if (iconButton(g, "dock-redo", cx, cy, HudIcons.REDO, canRedo())) redo();
        ui.tooltip("dock-redo", cx, cy, 18, bh, Lang.tr("core.hud.dock.redo"));
        cx += 18 + 3;
        g.rect(cx, cy + 3, 1, bh - 6, Theme.LINE2);
        cx += 4;
        if (dockButton(g, "dock-tpl", cx, cy, wTpl, bh, HudIcons.TEMPLATE, tpl, panels.menu() == HudEditorPanels.Menu.TEMPLATES)) {
            panels.toggleMenu(HudEditorPanels.Menu.TEMPLATES, cx, y, top);
        }
        cx += wTpl + gap;
        if (dockButton(g, "dock-share", cx, cy, wShare, bh, HudIcons.SHARE, share, panels.menu() == HudEditorPanels.Menu.SHARE)) {
            panels.toggleMenu(HudEditorPanels.Menu.SHARE, cx, y, top);
        }
        cx += wShare + 3;
        g.rect(cx, cy + 3, 1, bh - 6, Theme.LINE2);
        cx += 4;
        if (Widgets.button(ui, "dock-cancel", cx, cy, wCancel, bh, cancel, Widgets.Style.GHOST)) cancel();
        ui.tooltip("dock-cancel", cx, cy, wCancel, bh, Lang.tr("core.hud.cancel.tip"));
        cx += wCancel + gap;
        if (Widgets.button(ui, "dock-done", cx, cy, wDone, bh, done, Widgets.Style.PRIMARY)) done();
        // the dock as a whole swallows clicks (registered last: the buttons above win)
        ui.clickedNoFocus(x, y, total, dockH);

        // one hint line on the other side of the dock
        String hint = Lang.tr("core.hud.hint");
        String clipped = g.clip(hint, sw - left - 16);
        int hw = g.width(clipped);
        int hy = t > 0.5f ? y + dockH + 4 : y - 14;
        int hcx = x + total / 2;
        if (hcx - hw / 2 - 5 < left) hcx = left + hw / 2 + 5;
        g.rect(hcx - hw / 2 - 5, hy - 2, hw + 10, 12, Theme.alpha(0xC00B0B0E, shown));
        g.text(clipped, hcx - hw / 2, hy, Theme.alpha(Theme.MUTED, shown));
    }

    /** Flat dock button with an icon and a label. */
    boolean dockButton(Gfx g, Object key, int x, int y, int w, int h, String icon, String label, boolean active) {
        boolean clicked = ui.clicked(x, y, w, h);
        float hov = ui.anim(key, 1, 0f).to(ui.hover(x, y, w, h) ? 1f : 0f);
        int bg = active ? Theme.alpha(Theme.BRAND, 0.28f) : Theme.alpha(Theme.SURFACE3, hov);
        g.rect(x, y, w, h, bg);
        if (active) g.rect(x, y + h - 1, w, 1, Theme.BRAND);
        int fg = active ? 0xFFFFFFFF : Theme.mix(Theme.MUTED, Theme.TEXT, hov);
        Icons.draw(g, icon, x + 5, y + (h - 9) / 2, 1, active ? Theme.BRAND_HOVER : fg);
        g.text(label, x + 18, y + (h - 8) / 2, fg);
        return clicked;
    }

    boolean iconButton(Gfx g, Object key, int x, int y, String icon, boolean enabled) {
        int s = 18;
        boolean clicked = enabled && ui.clicked(x, y, s, s);
        float hov = ui.anim(key, 1, 0f).to(enabled && ui.hover(x, y, s, s) ? 1f : 0f);
        g.rect(x, y, s, s, Theme.alpha(Theme.SURFACE3, hov));
        Icons.draw(g, icon, x + 4, y + 4, 1, enabled ? Theme.mix(Theme.MUTED, Theme.TEXT, hov) : Theme.alpha(Theme.DIM, 0.6f));
        return clicked;
    }

    // ============================================================================================ input

    @Override
    protected boolean click(double mx, double my, int button) {
        if (panels.modal()) {
            UiHost.click(ui, mx, my, button);
            return true;
        }
        if (UiHost.click(ui, mx, my, button)) return true;
        if (panels.closeTransient()) return true;
        HudElement p = primary();
        int h = selection.size() == 1 && p != null && button == 0 ? handleAt(p, mx, my) : -1;
        HudElement hit = h >= 0 ? p : elementAt(mx, my);
        boolean ctrl = ClientCompat.controlDown();
        boolean shift = ClientCompat.shiftDown();
        if (button == 1) {
            if (hit == null) {
                panels.openCanvasMenu((int) mx, (int) my);
            } else {
                if (!isSelected(hit)) selectOnly(hit);
                panels.openContextMenu((int) mx, (int) my);
            }
            return true;
        }
        if (button != 0) return true;
        pressX = mx;
        pressY = my;
        if (h >= 0) {
            startResize(p, h);
            return true;
        }
        if (hit == null) {
            mode = MARQUEE;
            mqX1 = mqX2 = (float) mx;
            mqY1 = mqY2 = (float) my;
            marqueeBase.clear();
            if (ctrl || shift) marqueeBase.addAll(selection);
            else if (!selection.isEmpty()) {
                selection.clear();
                panels.selectionChanged();
            }
            return true;
        }
        pressed = hit;
        pressWasSelected = isSelected(hit);
        pressToggle = ctrl || shift;
        if (pressToggle) {
            if (!pressWasSelected) {
                selection.add(hit);
                panels.selectionChanged();
            }
        } else if (!pressWasSelected) {
            selectOnly(hit);
        }
        mode = PRESS;
        return true;
    }

    @Override
    protected boolean drag(double mx, double my, int button) {
        if (button != 0) return false;
        switch (mode) {
            case PRESS -> {
                if (Math.abs(mx - pressX) + Math.abs(my - pressY) >= 2) startMove();
                if (mode == MOVE) moveTo(mx, my);
            }
            case MOVE -> moveTo(mx, my);
            case RESIZE -> resizeTo(mx, my);
            case MARQUEE -> marquee(mx, my);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean release(double x, double y, int button) {
        UiHost.release(ui);
        switch (mode) {
            case PRESS -> {
                if (pressToggle && pressWasSelected) {
                    selection.remove(pressed);
                    panels.selectionChanged();
                } else if (!pressToggle && selection.size() > 1) {
                    selectOnly(pressed);
                }
            }
            case MOVE, RESIZE -> {
                if (dragBefore != null && !dragBefore.sameAs(HudLayout.capture())) {
                    push(dragBefore);
                    lastKey = null;
                }
                dragBefore = null;
            }
            default -> {
            }
        }
        mode = IDLE;
        pressed = null;
        guideCount = 0;
        distCount = 0;
        anchorDotX = Float.NaN;
        return true;
    }

    @Override
    protected boolean scroll(double mx, double my, double amount) {
        if (panels.covers((int) mx, (int) my) || panels.modal()) {
            UiHost.scroll(ui, amount);
            return true;
        }
        HudElement e = elementAt(mx, my);
        if (e == null) {
            if (selection.isEmpty()) return true;
            e = primary();
        }
        if (!isSelected(e)) selectOnly(e);
        checkpoint(selection);
        float step = amount > 0 ? 0.05f : -0.05f;
        for (int i = 0; i < selection.size(); i++) {
            HudElement s = selection.get(i);
            HudLayout.scaleTo(s, HudLayout.placement(s).scale + step, width, height);
        }
        return true;
    }

    @Override
    protected boolean key(int keyCode) {
        if (UiHost.anyInputFocused(ui)) {
            if (keyCode == Keys.ESCAPE) {
                ui.blurInputs();
                panels.escape();
                return true;
            }
            if (keyCode == Keys.ENTER || keyCode == 335) return panels.enter();
            return false;
        }
        if (keyCode == Keys.ESCAPE) {
            // Esc closes the topmost panel or menu; otherwise it saves and leaves (like every Minecraft screen)
            if (panels.escape()) return true;
            done();
            return true;
        }
        if (panels.modal()) {
            if (keyCode == Keys.ENTER || keyCode == 335) return panels.enter();
            return ui.key(keyCode, ClientCompat.shiftDown(), false);
        }
        boolean ctrl = ClientCompat.controlDown();
        boolean shift = ClientCompat.shiftDown();
        int letter = ClientCompat.layoutLetter(keyCode);
        if (ctrl) {
            switch (letter) {
                case 'Z' -> {
                    if (shift) redo();
                    else undo();
                    return true;
                }
                case 'Y' -> {
                    redo();
                    return true;
                }
                case 'A' -> {
                    selection.clear();
                    selection.addAll(canvas);
                    panels.selectionChanged();
                    return true;
                }
                case 'D' -> {
                    duplicateSelection();
                    return true;
                }
                default -> {
                }
            }
        }
        if (selection.isEmpty()) return false;
        int step = shift ? 10 : 1;
        switch (keyCode) {
            case Keys.LEFT -> nudge(-step, 0);
            case Keys.RIGHT -> nudge(step, 0);
            case Keys.UP -> nudge(0, -step);
            case Keys.DOWN -> nudge(0, step);
            case Keys.DELETE, Keys.BACKSPACE -> hideSelection();
            default -> {
                if (letter == 'H') hideSelection();
                else if (letter == 'R') resetSelection();
                else return false;
            }
        }
        return true;
    }

    // ============================================================================================ actions

    private void nudge(int dx, int dy) {
        checkpoint("nudge");
        for (int i = 0; i < selection.size(); i++) {
            HudElement e = selection.get(i);
            HudLayout.Box b = HudLayout.box(e, width, height, tmp);
            HudLayout.moveTo(e, b.x + dx, b.y + dy, width, height);
        }
    }

    void hideSelection() {
        if (selection.isEmpty()) return;
        checkpoint(null);
        for (HudElement e : new ArrayList<>(selection)) {
            if (e.isCopy()) HudLayout.removeCopy(e);
            else HudLayout.setHidden(e, true);
        }
        int n = selection.size();
        selection.clear();
        panels.selectionChanged();
        panels.toast(n == 1 ? Lang.tr("core.hud.toast.hidden") : Lang.tr("core.hud.toast.hiddenMany", n));
    }

    void resetSelection() {
        if (selection.isEmpty()) return;
        checkpoint(null);
        for (int i = 0; i < selection.size(); i++) HudLayout.reset(selection.get(i));
        panels.toast(Lang.tr("core.hud.toast.reset"));
    }

    void duplicateSelection() {
        List<HudElement> made = new ArrayList<>();
        boolean any = false;
        for (HudElement e : selection) any |= e.duplicable();
        if (!any) return;
        checkpoint(null);
        for (HudElement e : new ArrayList<>(selection)) {
            if (!e.duplicable()) continue;
            HudElement c = HudLayout.duplicate(e, width, height);
            if (c != null) made.add(c);
        }
        if (!made.isEmpty()) {
            selection.clear();
            selection.addAll(made);
            panels.selectionChanged();
            panels.toast(Lang.tr("core.hud.toast.duplicated"));
        }
    }

    /** Shows a hidden element again (library) and selects it. */
    void addToCanvas(HudElement e) {
        checkpoint(null);
        HudLayout.setHidden(e, false);
        HudLayout.Box b = HudLayout.box(e, width, height, tmp);
        // keep it clear of the library panel
        if (panels.libraryOpen() && b.x < panels.libraryRight() + 4) HudLayout.moveTo(e, panels.libraryRight() + 8, b.y, width, height);
        refreshLists();
        selectOnly(e);
    }

    void applyState(HudLayout.State s, boolean complete, String toastKey) {
        checkpoint(null);
        HudLayout.apply(s, complete);
        lastKey = null;
        selection.clear();
        panels.selectionChanged();
        if (toastKey != null) panels.toast(Lang.tr(toastKey));
    }

    // ============================================================================================ move

    private void startMove() {
        mode = MOVE;
        dragBefore = HudLayout.capture();
        if (startX.length < selection.size()) {
            startX = new float[selection.size() * 2];
            startY = new float[selection.size() * 2];
        }
        for (int i = 0; i < selection.size(); i++) {
            HudLayout.Box b = HudLayout.box(selection.get(i), width, height, tmp);
            startX[i] = b.x;
            startY[i] = b.y;
        }
        groupBox(groupStart);
    }

    private void moveTo(double mx, double my) {
        float dx = (float) (mx - pressX), dy = (float) (my - pressY);
        float gx = groupStart.x + dx, gy = groupStart.y + dy;
        gx = Math.max(0, Math.min(width - groupStart.w, gx));
        gy = Math.max(0, Math.min(height - groupStart.h, gy));
        guideCount = 0;
        boolean free = ClientCompat.controlDown() || altDown();
        if (!free) {
            gx = snap(true, gx, groupStart.w, gy, groupStart.h);
            gy = snap(false, gy, groupStart.h, gx, groupStart.w);
        }
        float ox = gx - groupStart.x, oy = gy - groupStart.y;
        for (int i = 0; i < selection.size(); i++) {
            HudLayout.moveTo(selection.get(i), startX[i] + ox, startY[i] + oy, width, height);
        }
        groupBox(group);
        measure(group, primary());
    }

    private static boolean altDown() {
        try {
            //? if >=1.21.9 {
            return net.minecraft.client.Minecraft.getInstance().hasAltDown();
            //?} else
            /*return Screen.hasAltDown();*/
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Bounding box of the selection. */
    private void groupBox(HudLayout.Box out) {
        float x1 = Float.MAX_VALUE, y1 = Float.MAX_VALUE, x2 = -Float.MAX_VALUE, y2 = -Float.MAX_VALUE;
        for (int i = 0; i < selection.size(); i++) {
            HudLayout.Box b = HudLayout.box(selection.get(i), width, height, tmp);
            x1 = Math.min(x1, b.x);
            y1 = Math.min(y1, b.y);
            x2 = Math.max(x2, b.x + b.w);
            y2 = Math.max(y2, b.y + b.h);
        }
        if (x1 == Float.MAX_VALUE) x1 = y1 = x2 = y2 = 0;
        out.x = x1;
        out.y = y1;
        out.w = x2 - x1;
        out.h = y2 - y1;
        out.scale = 1;
    }

    /**
     * Snaps a box edge/centre on one axis to the screen (edges, margin, centre) and to the other elements' edges and
     * centres. Records the guide line. {@code ox}/{@code ow} = the box on the other axis (for the guide's extent).
     */
    private float snap(boolean xAxis, float pos, float size, float ox, float ow) {
        float screen = xAxis ? width : height;
        float best = pos;
        float bestD = SNAP + 0.5f;
        float guideAt = Float.NaN, gFrom = 0, gTo = 0;
        // screen: margin, edge, centre
        float[] s = SCREEN_TARGETS;
        s[0] = MARGIN; s[1] = 0; s[2] = screen - MARGIN - size; s[3] = screen - size; s[4] = screen / 2f - size / 2f;
        for (int i = 0; i < 5; i++) {
            float d = Math.abs(pos - s[i]);
            if (d < bestD) {
                bestD = d;
                best = s[i];
                if (i == 4) {
                    guideAt = screen / 2f;
                    gFrom = 0;
                    gTo = xAxis ? height : width;
                } else {
                    guideAt = Float.NaN;
                }
            }
        }
        for (int i = 0; i < canvas.size(); i++) {
            HudElement o = canvas.get(i);
            if (isSelected(o)) continue;
            HudLayout.Box b = HudLayout.box(o, width, height, tmp2);
            float a = xAxis ? b.x : b.y, l = xAxis ? b.w : b.h;
            float oa = xAxis ? b.y : b.x, ol = xAxis ? b.h : b.w;
            // my start/end/centre to their start/end/centre
            float[] t = OTHER_TARGETS;
            t[0] = a; t[1] = a + l - size; t[2] = a + l; t[3] = a - size; t[4] = a + l / 2f - size / 2f;
            t[5] = a + l + 2; t[6] = a - size - 2;
            float[] line = OTHER_LINES;
            line[0] = a; line[1] = a + l; line[2] = a + l; line[3] = a; line[4] = a + l / 2f; line[5] = a + l + 1; line[6] = a - 1;
            for (int k = 0; k < 7; k++) {
                float d = Math.abs(pos - t[k]);
                if (d < bestD) {
                    bestD = d;
                    best = t[k];
                    guideAt = line[k];
                    gFrom = Math.min(oa, ox) - 4;
                    gTo = Math.max(oa + ol, ox + ow) + 4;
                }
            }
        }
        if (!Float.isNaN(guideAt)) addGuide(xAxis, guideAt, gFrom, gTo);
        return best;
    }

    private static final float[] SCREEN_TARGETS = new float[5];
    private static final float[] OTHER_TARGETS = new float[7];
    private static final float[] OTHER_LINES = new float[7];

    // ============================================================================================ resize

    private void startResize(HudElement e, int h) {
        mode = RESIZE;
        handle = h;
        dragBefore = HudLayout.capture();
        HudLayout.Box b = HudLayout.box(e, width, height, resizeStart);
        HudLayout.Placement p = HudLayout.placement(e);
        resizeScale = b.scale;
        resizeBw = HudLayout.boxWidth(e);
        resizeBh = HudLayout.boxHeight(e);
        sizeChipKey = Integer.MIN_VALUE;
        if (p.bw == 0 && p.bh == 0) {
            resizeBw = e.width();
            resizeBh = e.height();
        }
    }

    private void resizeTo(double mx, double my) {
        HudElement e = primary();
        if (e == null) return;
        guideCount = 0;
        int sx = HX[handle], sy = HY[handle];
        float dmx = (float) (mx - pressX), dmy = (float) (my - pressY);
        HudLayout.Box s = resizeStart;
        boolean snapOn = !ClientCompat.controlDown() && !altDown();
        float right = s.x + s.w, bottom = s.y + s.h;
        if (ClientCompat.shiftDown() && e.isWidget()) {
            // free: change the box size (unscaled px), the opposite edges stay
            float nw = s.w + sx * dmx, nh = s.h + sy * dmy;
            if (snapOn && sx != 0) nw = snapEdge(true, sx > 0 ? s.x + nw : right - nw, sx > 0 ? s.x : right, sx, s.y, s.h);
            if (snapOn && sy != 0) nh = snapEdge(false, sy > 0 ? s.y + nh : bottom - nh, sy > 0 ? s.y : bottom, sy, s.x, s.w);
            int bw = sx == 0 ? HudLayout.placement(e).bw : Math.max(4, Math.round(nw / resizeScale));
            int bh = sy == 0 ? HudLayout.placement(e).bh : Math.max(4, Math.round(nh / resizeScale));
            HudLayout.setSize(e, bw, bh);
            float w = HudLayout.boxWidth(e) * resizeScale, h = HudLayout.boxHeight(e) * resizeScale;
            float x = sx < 0 ? right - w : s.x;
            float y = sy < 0 ? bottom - h : s.y;
            HudLayout.moveTo(e, x, y, width, height);
        } else {
            float f;
            if (sx != 0 && sy != 0) {
                float fx = (s.w + sx * dmx) / s.w, fy = (s.h + sy * dmy) / s.h;
                f = (fx * s.w * s.w + fy * s.h * s.h) / (s.w * s.w + s.h * s.h);
            } else if (sx != 0) {
                f = (s.w + sx * dmx) / s.w;
            } else {
                f = (s.h + sy * dmy) / s.h;
            }
            float scale = HudLayout.clampScale(e, resizeScale * f);
            if (snapOn) {
                // the moving edge snaps; the scale follows it exactly
                float w = s.w * scale / resizeScale, h = s.h * scale / resizeScale;
                if (sx != 0) {
                    float edge = sx > 0 ? s.x + w : right - w;
                    float nw = snapEdge(true, edge, sx > 0 ? s.x : right, sx, s.y, h);
                    if (nw != w) scale = Math.max(e.minScale(), Math.min(e.maxScale(), resizeScale * nw / s.w));
                } else {
                    float edge = sy > 0 ? s.y + h : bottom - h;
                    float nh = snapEdge(false, edge, sy > 0 ? s.y : bottom, sy, s.x, w);
                    if (nh != h) scale = Math.max(e.minScale(), Math.min(e.maxScale(), resizeScale * nh / s.h));
                }
            }
            HudLayout.Placement p = HudLayout.placement(e);
            p.scale = scale;
            float w = HudLayout.boxWidth(e) * scale, h = HudLayout.boxHeight(e) * scale;
            float x = sx < 0 ? right - w : sx > 0 ? s.x : s.x + s.w / 2f - w / 2f;
            float y = sy < 0 ? bottom - h : sy > 0 ? s.y : s.y + s.h / 2f - h / 2f;
            HudLayout.moveTo(e, x, y, width, height);
        }
        HudLayout.Box b = HudLayout.box(e, width, height, tmp);
        measure(b, e);
        distCount = 0;
        int key = Math.round(b.w) * 100_000 + Math.round(b.h) * 100 + Math.round(b.scale * 100) % 100;
        if (key != sizeChipKey) {
            sizeChipKey = key;
            sizeChip = Math.round(b.w / b.scale) + " × " + Math.round(b.h / b.scale) + "  ·  " + Math.round(b.scale * 100) + "%";
        }
    }

    /** Snaps a moving edge; returns the new size (distance from the fixed edge). */
    private float snapEdge(boolean xAxis, float edge, float fixed, int dir, float ox, float ow) {
        float screen = xAxis ? width : height;
        float best = edge, bestD = SNAP + 0.5f, guide = Float.NaN, gFrom = 0, gTo = 0;
        float[] s = SCREEN_TARGETS;
        s[0] = MARGIN; s[1] = screen - MARGIN; s[2] = screen / 2f; s[3] = 0; s[4] = screen;
        for (int i = 0; i < 5; i++) {
            float d = Math.abs(edge - s[i]);
            if (d < bestD) {
                bestD = d;
                best = s[i];
                guide = i == 2 ? s[i] : Float.NaN;
                gFrom = 0;
                gTo = xAxis ? height : width;
            }
        }
        for (int i = 0; i < canvas.size(); i++) {
            HudElement o = canvas.get(i);
            if (isSelected(o)) continue;
            HudLayout.Box b = HudLayout.box(o, width, height, tmp2);
            float a = xAxis ? b.x : b.y, l = xAxis ? b.w : b.h;
            float oa = xAxis ? b.y : b.x, ol = xAxis ? b.h : b.w;
            float[] t = OTHER_TARGETS;
            t[0] = a; t[1] = a + l; t[2] = a + l / 2f;
            for (int k = 0; k < 3; k++) {
                float d = Math.abs(edge - t[k]);
                if (d < bestD) {
                    bestD = d;
                    best = t[k];
                    guide = t[k];
                    gFrom = Math.min(oa, ox) - 4;
                    gTo = Math.max(oa + ol, ox + ow) + 4;
                }
            }
        }
        if (!Float.isNaN(guide)) addGuide(xAxis, guide, gFrom, gTo);
        return Math.max(4, dir > 0 ? best - fixed : fixed - best);
    }

    // ============================================================================================ marquee

    private void marquee(double mx, double my) {
        mqX2 = (float) mx;
        mqY2 = (float) my;
        float x1 = Math.min(mqX1, mqX2), y1 = Math.min(mqY1, mqY2), x2 = Math.max(mqX1, mqX2), y2 = Math.max(mqY1, mqY2);
        selection.clear();
        selection.addAll(marqueeBase);
        for (int i = 0; i < canvas.size(); i++) {
            HudElement e = canvas.get(i);
            HudLayout.Box b = HudLayout.box(e, width, height, tmp);
            if (b.x < x2 && b.x + b.w > x1 && b.y < y2 && b.y + b.h > y1 && !isSelected(e)) selection.add(e);
        }
        panels.selectionChanged();
    }

    // ============================================================================================ hit testing

    HudElement elementAt(double mx, double my) {
        // the selected ones win when they overlap; then topmost (last drawn) first
        for (int i = selection.size() - 1; i >= 0; i--) {
            HudLayout.Box b = HudLayout.box(selection.get(i), width, height, tmp2);
            if (mx >= b.x - 2 && mx < b.x + b.w + 2 && my >= b.y - 2 && my < b.y + b.h + 2) return selection.get(i);
        }
        for (int i = canvas.size() - 1; i >= 0; i--) {
            HudLayout.Box b = HudLayout.box(canvas.get(i), width, height, tmp2);
            if (mx >= b.x && mx < b.x + b.w && my >= b.y && my < b.y + b.h) return canvas.get(i);
        }
        return null;
    }

    // ============================================================================================ files

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (paths == null || paths.isEmpty()) return;
        panels.importFile(paths.get(0));
    }

    // ============================================================================================ test hooks

    /** Selects an element by id (tests and screenshots). */
    public void select(String id) {
        HudElement e = HudLayout.get(id);
        selectOnly(e);
    }

    public int selectionCount() {
        return selection.size();
    }

    /** Adds an element to the selection (tests). */
    public void addSelection(String id) {
        HudElement e = HudLayout.get(id);
        if (e != null && !isSelected(e)) selection.add(e);
        panels.selectionChanged();
    }

    /** Test hooks for the panels: "library", "style", "context", "templates", "share", "intro", "import". */
    public void testOpen(String what, int x, int y) {
        panels.testOpen(what, x, y);
    }

    public void testCloseIntro() {
        panels.closeIntro();
    }
}
