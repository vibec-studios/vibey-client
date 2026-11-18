package dev.henny.vibeyclient.ui.hud;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.BaseScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.UiHost;
import dev.henny.vibeyclient.ui.Widgets;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

/**
 * Move and scale HUD elements. Drag an element to move it (it snaps to the screen edges, the centre lines and the
 * other elements; hold Ctrl to place freely), drag its corner or scroll over it to resize, right-click to reset it.
 * Arrow keys nudge the selected element (Shift: 10 px), H hides/shows it, R resets it. Esc or "Done" saves, "Cancel"
 * restores what was there when the editor opened. The world keeps running behind it.
 */
public final class HudEditorScreen extends BaseScreen {
    private static final int SNAP = 4;
    private static final int MARGIN = 4;
    private static final int HANDLE = 5;

    /** While open, the elements are only drawn by the editor (the normal HUD skips them). */
    static boolean open;

    private final Screen parent;
    private final Map<String, HudLayout.Placement> before;
    private Ui ui;
    private HudElement selected;
    private HudElement hovered;
    private boolean moving;
    private boolean resizing;
    private float grabX;
    private float grabY;
    private float startScale;
    private float startDist;
    private boolean saved;
    // guides of the current snap (screen coordinates, NaN = none)
    private float guideX = Float.NaN;
    private float guideY = Float.NaN;
    private final HudLayout.Box tmp = new HudLayout.Box();
    private final HudLayout.Box tmp2 = new HudLayout.Box();

    public HudEditorScreen(Screen parent) {
        super(Component.literal(Lang.tr("core.hud.title")));
        this.parent = parent;
        this.before = HudLayout.snapshot();
    }

    @Override
    protected void init() {
        open = true;
        if (ui == null) ui = UiHost.create(b -> {});
        else UiHost.reattach(ui);
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

    private void done() {
        saved = true;
        HudLayout.save();
        open = false;
        ClientCompat.openScreen(parent);
    }

    private void cancel() {
        HudLayout.restore(before);
        saved = true;
        open = false;
        ClientCompat.openScreen(parent);
    }

    private List<HudElement> visibleElements() {
        List<HudElement> all = HudLayout.elements();
        all = new java.util.ArrayList<>(all);
        all.removeIf(e -> !e.available());
        return all;
    }

    // ============================================================================================ drawing

    @Override
    protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
        UiHost.begin(ui, g, mouseX, mouseY, delta);
        int sw = width;
        int sh = height;
        boolean inWorld = minecraft != null && minecraft.level != null;
        g.rect(0, 0, sw, sh, inWorld ? 0x38000000 : 0xE0101014);
        grid(g, sw, sh);

        List<HudElement> els = visibleElements();
        hovered = null;
        for (HudElement e : els) {
            HudLayout.Box b = HudLayout.box(e, sw, sh, tmp);
            if (mouseX >= b.x && mouseX < b.x + b.w && mouseY >= b.y && mouseY < b.y + b.h) hovered = e;
        }
        if (moving || resizing) hovered = selected;

        for (HudElement e : els) {
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
        g.layer();
        for (HudElement e : els) frame(g, e, HudLayout.box(e, sw, sh, tmp));

        // snap guides
        if (!Float.isNaN(guideX)) g.rect(Math.round(guideX), 0, 1, sh, Theme.BRAND2);
        if (!Float.isNaN(guideY)) g.rect(0, Math.round(guideY), sw, 1, Theme.BRAND2);

        if (els.isEmpty()) {
            g.textCenter(Lang.tr("core.hud.empty"), sw / 2, sh / 2 - 4, Theme.MUTED);
        }
        toolbar(g, sw, sh, els);
        UiHost.end(ui);
    }

    /** Faint thirds and centre lines, like a camera grid. */
    private static void grid(Gfx g, int sw, int sh) {
        int c = 0x14FFFFFF;
        g.rect(sw / 3, 0, 1, sh, c);
        g.rect(sw * 2 / 3, 0, 1, sh, c);
        g.rect(0, sh / 3, sw, 1, c);
        g.rect(0, sh * 2 / 3, sw, 1, c);
        int m = 0x22FFFFFF;
        for (int y = 0; y < sh; y += 6) g.rect(sw / 2, y, 1, 3, m);
        for (int x = 0; x < sw; x += 6) g.rect(x, sh / 2, 3, 1, m);
    }

    private void frame(Gfx g, HudElement e, HudLayout.Box b) {
        boolean sel = e == selected;
        boolean hov = e == hovered;
        HudLayout.Placement p = HudLayout.placement(e);
        int x = (int) Math.floor(b.x);
        int y = (int) Math.floor(b.y);
        int w = Math.max(4, Math.round(b.w));
        int h = Math.max(4, Math.round(b.h));
        if (p.hidden) {
            g.rect(x, y, w, h, 0xB0000000);
            g.textCenter(Lang.tr("core.hud.hidden"), x + w / 2, y + h / 2 - 4, Theme.DIM);
        }
        int line = sel ? Theme.BRAND : hov ? Theme.alpha(Theme.BRAND, 0.7f) : 0x66FFFFFF;
        if (!sel && !hov) {
            // dashed outline for the resting state
            dashed(g, x, y, w, h, line);
        } else {
            g.outline(x, y, w, h, line);
            if (sel) g.rect(x, y, w, h, Theme.alpha(Theme.BRAND, 0.08f));
        }
        // name tag on top (or inside when there is no room above)
        String label = Lang.tr(e.labelKey());
        if (sel || hov) label += "  " + Math.round(p.scale * 100) + "%";
        int tw = g.width(label) + 8;
        // above the element, else below it, else inside its top edge
        int ty = y - 12 >= 0 ? y - 12 : y + h + 1 + 11 <= height ? y + h + 1 : y + 1;
        int tx = Math.max(0, Math.min(width - tw, x));
        g.rect(tx, ty, tw, 11, sel ? Theme.BRAND : 0xCC0B0B0E);
        g.text(label, tx + 4, ty + 2, sel ? 0xFFFFFFFF : Theme.MUTED);
        // resize handle bottom right
        if (sel || hov) {
            int hx = x + w - HANDLE + 1;
            int hy = y + h - HANDLE + 1;
            g.rect(hx, hy, HANDLE, HANDLE, sel ? Theme.BRAND : Theme.TEXT);
            g.rect(hx + 1, hy + 1, HANDLE - 2, HANDLE - 2, sel ? 0xFFFFFFFF : Theme.SURFACE3);
        }
    }

    private static void dashed(Gfx g, int x, int y, int w, int h, int c) {
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

    private void toolbar(Gfx g, int sw, int sh, List<HudElement> els) {
        String done = Lang.tr("core.hud.done");
        String cancel = Lang.tr("core.hud.cancel");
        String reset = Lang.tr("core.hud.resetAll");
        int bh = 18;
        int pad = 6;
        int wDone = Math.max(56, g.width(done) + 16);
        int wCancel = Math.max(56, g.width(cancel) + 16);
        int wReset = Math.max(56, g.width(reset) + 16);
        int selW = 0;
        String hide = null, rst = null;
        if (selected != null) {
            hide = Lang.tr(HudLayout.placement(selected).hidden ? "core.hud.show" : "core.hud.hide");
            rst = Lang.tr("core.hud.reset");
            selW = 18 + 40 + 18 + 4 + Math.max(40, g.width(rst) + 12) + 4 + Math.max(40, g.width(hide) + 12) + 12;
        }
        int total = wReset + wCancel + wDone + 8 + selW + pad * 2;
        int x = (sw - total) / 2;
        // bottom, unless the selected element sits there
        int y = sh - bh - pad * 2 - 6;
        if (selected != null) {
            HudLayout.Box b = HudLayout.box(selected, sw, sh, tmp2);
            if (b.y + b.h > y - 14) y = 6;
        }
        int barH = bh + pad * 2;
        g.rect(x, y, total, barH, 0xF00B0B0E);
        g.outline(x, y, total, barH, Theme.LINE2);
        int cx = x + pad;
        int cy = y + pad;
        if (selected != null) {
            HudLayout.Placement p = HudLayout.placement(selected);
            if (Widgets.button(ui, "hud-minus", cx, cy, 18, bh, "−", Widgets.Style.SECONDARY)) {
                HudLayout.scaleTo(selected, p.scale - 0.1f, sw, sh);
            }
            cx += 18;
            g.rect(cx, cy, 40, bh, Theme.SURFACE);
            g.textCenter(Math.round(p.scale * 100) + "%", cx + 20, cy + 5, Theme.TEXT);
            cx += 40;
            if (Widgets.button(ui, "hud-plus", cx, cy, 18, bh, "+", Widgets.Style.SECONDARY)) {
                HudLayout.scaleTo(selected, p.scale + 0.1f, sw, sh);
            }
            cx += 22;
            int rw = Math.max(40, g.width(rst) + 12);
            if (Widgets.button(ui, "hud-reset", cx, cy, rw, bh, rst, Widgets.Style.SECONDARY)) HudLayout.reset(selected);
            cx += rw + 4;
            int hw = Math.max(40, g.width(hide) + 12);
            if (Widgets.button(ui, "hud-hide", cx, cy, hw, bh, hide, Widgets.Style.SECONDARY)) {
                HudLayout.setHidden(selected, !p.hidden);
            }
            cx += hw + 6;
            g.rect(cx, cy - 2, 1, bh + 4, Theme.LINE2);
            cx += 6;
        }
        if (Widgets.button(ui, "hud-resetall", cx, cy, wReset, bh, reset, Widgets.Style.DANGER)) {
            HudLayout.resetAll();
        }
        cx += wReset + 4;
        if (Widgets.button(ui, "hud-cancel", cx, cy, wCancel, bh, cancel, Widgets.Style.SECONDARY)) cancel();
        cx += wCancel + 4;
        if (Widgets.button(ui, "hud-done", cx, cy, wDone, bh, done, Widgets.Style.PRIMARY)) done();

        String hint = Lang.tr(els.isEmpty() ? "core.hud.hintEmpty" : "core.hud.hint");
        int hy = y == 6 ? y + barH + 4 : y - 12;
        String clipped = g.clip(hint, sw - 16);
        int hw = g.width(clipped) + 8;
        g.rect(sw / 2 - hw / 2, hy - 2, hw, 12, 0xA0000000);
        g.textCenter(clipped, sw / 2, hy, Theme.MUTED);
    }

    // ============================================================================================ input

    @Override
    protected boolean click(double mx, double my, int button) {
        if (UiHost.click(ui, mx, my, button)) return true;
        HudElement hit = elementAt(mx, my);
        if (hit == null) {
            selected = null;
            return true;
        }
        selected = hit;
        HudLayout.Box b = HudLayout.box(hit, width, height, tmp);
        if (button == 1) {
            HudLayout.reset(hit);
            return true;
        }
        if (button != 0) return true;
        boolean onHandle = mx >= b.x + b.w - HANDLE - 2 && my >= b.y + b.h - HANDLE - 2;
        if (onHandle) {
            resizing = true;
            startScale = b.scale;
            startDist = (float) Math.max(8, Math.hypot(mx - b.x, my - b.y));
        } else {
            moving = true;
            grabX = (float) (mx - b.x);
            grabY = (float) (my - b.y);
        }
        return true;
    }

    @Override
    protected boolean drag(double mx, double my, int button) {
        if (selected == null || button != 0) return false;
        if (moving) {
            float x = (float) mx - grabX;
            float y = (float) my - grabY;
            HudLayout.Box b = HudLayout.box(selected, width, height, tmp);
            guideX = Float.NaN;
            guideY = Float.NaN;
            if (!ClientCompat.controlDown()) {
                x = snapX(x, b.w);
                y = snapY(y, b.h);
            }
            HudLayout.moveTo(selected, x, y, width, height);
            return true;
        }
        if (resizing) {
            HudLayout.Box b = HudLayout.box(selected, width, height, tmp);
            float d = (float) Math.max(4, Math.hypot(mx - b.x, my - b.y));
            float s = startScale * d / startDist;
            // keep the top left corner where it is while resizing from the corner
            float x = b.x;
            float y = b.y;
            HudLayout.Placement p = HudLayout.placement(selected);
            p.scale = Math.max(selected.minScale(), Math.min(selected.maxScale(), Math.round(s * 20f) / 20f));
            HudLayout.moveTo(selected, x, y, width, height);
            return true;
        }
        return false;
    }

    @Override
    protected boolean release(double x, double y, int button) {
        UiHost.release(ui);
        moving = false;
        resizing = false;
        guideX = Float.NaN;
        guideY = Float.NaN;
        return true;
    }

    @Override
    protected boolean scroll(double mx, double my, double amount) {
        HudElement e = elementAt(mx, my);
        if (e == null) e = selected;
        if (e == null) {
            UiHost.scroll(ui, amount);
            return true;
        }
        selected = e;
        HudLayout.scaleTo(e, HudLayout.placement(e).scale + (amount > 0 ? 0.05f : -0.05f), width, height);
        return true;
    }

    @Override
    protected boolean key(int keyCode) {
        if (keyCode == Keys.ESCAPE) {
            done();
            return true;
        }
        if (selected == null) return false;
        int step = ClientCompat.shiftDown() ? 10 : 1;
        HudLayout.Box b = HudLayout.box(selected, width, height, tmp);
        switch (keyCode) {
            case Keys.LEFT -> HudLayout.moveTo(selected, b.x - step, b.y, width, height);
            case Keys.RIGHT -> HudLayout.moveTo(selected, b.x + step, b.y, width, height);
            case Keys.UP -> HudLayout.moveTo(selected, b.x, b.y - step, width, height);
            case Keys.DOWN -> HudLayout.moveTo(selected, b.x, b.y + step, width, height);
            case Keys.H -> HudLayout.setHidden(selected, !HudLayout.placement(selected).hidden);
            case Keys.R -> HudLayout.reset(selected);
            default -> {
                return false;
            }
        }
        return true;
    }

    private HudElement elementAt(double mx, double my) {
        List<HudElement> els = visibleElements();
        // topmost (last drawn) first; the selected one wins when they overlap
        if (selected != null && els.contains(selected)) {
            HudLayout.Box b = HudLayout.box(selected, width, height, tmp);
            if (mx >= b.x - 2 && mx < b.x + b.w + 2 && my >= b.y - 2 && my < b.y + b.h + 2) return selected;
        }
        for (int i = els.size() - 1; i >= 0; i--) {
            HudLayout.Box b = HudLayout.box(els.get(i), width, height, tmp);
            if (mx >= b.x && mx < b.x + b.w && my >= b.y && my < b.y + b.h) return els.get(i);
        }
        return null;
    }

    // ============================================================================================ snapping

    private float snapX(float x, float w) {
        float best = Float.NaN;
        float bestD = SNAP + 1;
        float guide = Float.NaN;
        // candidates: [target position of the left edge, guide line]
        float[][] cands = {
                {MARGIN, MARGIN}, {width - MARGIN - w, width - MARGIN}, {width / 2f - w / 2f, width / 2f},
                {0, 0}, {width - w, width}
        };
        for (float[] c : cands) {
            float d = Math.abs(x - c[0]);
            if (d < bestD) {
                bestD = d;
                best = c[0];
                guide = c[1];
            }
        }
        for (HudElement o : visibleElements()) {
            if (o == selected) continue;
            HudLayout.Box b = HudLayout.box(o, width, height, tmp2);
            float[][] edges = {{b.x, b.x}, {b.x + b.w - w, b.x + b.w}, {b.x + b.w, b.x + b.w}, {b.x - w, b.x},
                    {b.x + b.w / 2f - w / 2f, b.x + b.w / 2f}};
            for (float[] c : edges) {
                float d = Math.abs(x - c[0]);
                if (d < bestD) {
                    bestD = d;
                    best = c[0];
                    guide = c[1];
                }
            }
        }
        if (Float.isNaN(best)) return x;
        guideX = guide;
        return best;
    }

    private float snapY(float y, float h) {
        float best = Float.NaN;
        float bestD = SNAP + 1;
        float guide = Float.NaN;
        float[][] cands = {
                {MARGIN, MARGIN}, {height - MARGIN - h, height - MARGIN}, {height / 2f - h / 2f, height / 2f},
                {0, 0}, {height - h, height}
        };
        for (float[] c : cands) {
            float d = Math.abs(y - c[0]);
            if (d < bestD) {
                bestD = d;
                best = c[0];
                guide = c[1];
            }
        }
        for (HudElement o : visibleElements()) {
            if (o == selected) continue;
            HudLayout.Box b = HudLayout.box(o, width, height, tmp2);
            float[][] edges = {{b.y, b.y}, {b.y + b.h - h, b.y + b.h}, {b.y + b.h, b.y + b.h}, {b.y - h, b.y},
                    {b.y + b.h / 2f - h / 2f, b.y + b.h / 2f}};
            for (float[] c : edges) {
                float d = Math.abs(y - c[0]);
                if (d < bestD) {
                    bestD = d;
                    best = c[0];
                    guide = c[1];
                }
            }
        }
        if (Float.isNaN(best)) return y;
        guideY = guide;
        return best;
    }

    // ============================================================================================ test hooks

    /** Selects an element by id (tests and screenshots). */
    public void select(String id) {
        selected = HudLayout.get(id);
    }
}
