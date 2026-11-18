package dev.henny.vibeyclient.module.pvp;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.setting.Setting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.BaseScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The crosshair editor: a panel on the right, the crosshair itself stays where it is in the game. In a world the
 * real HUD crosshair shows live in the middle of the screen behind the panel (the screen draws no background and
 * keeps the game's GUI scale, so the size is exactly the in-game size); on the title screen a sample scene sits behind
 * it. The panel has a 4× magnified preview over four backgrounds, the shapes as tiles and every setting. Changes apply
 * (and save) at once; Reset puts the defaults back, Done / Esc goes back.
 */
final class CrosshairEditorScreen extends BaseScreen {
    private static final int PANEL_W = 214, PAD = 10, ROW = 22, MAG = 4;
    private static final int[] SCENE = {0xFF7FB2FF, 0xFF5D9B3A, 0xFF7D7D7D, 0xFFF2F6FA};
    private static final Object RESET = new Object(), DONE = new Object();

    private final Screen parent;
    private final PvpModule m;
    private final Ui ui = new Ui(box -> {});
    private final ScrollArea scroll = new ScrollArea();
    private final Crosshair.Style style = new Crosshair.Style();
    private final Crosshair.Style tile = new Crosshair.Style();
    private final Object[] shapeKeys = new Object[Crosshair.Shape.values().length];

    CrosshairEditorScreen(Screen parent, PvpModule module) {
        super(Component.literal("Crosshair"));
        this.parent = parent;
        this.m = module;
        for (int i = 0; i < shapeKeys.length; i++) shapeKeys[i] = new Object();
    }

    @Override
    protected void draw(Gfx g, int mx, int my, float delta) {
        ui.begin(g, mx, my, delta);
        boolean inWorld = Minecraft.getInstance().level != null;
        int px = width - PANEL_W;
        if (!inWorld) scene(g, px);
        else if (!m.isEnabled() || crosshairBlocked()) {
            // the HUD shows vanilla's crosshair then: show the edited one in the middle anyway
            Crosshair.draw(g, m.crosshairStyle(style), Crosshair.centre(width), Crosshair.centre(height), 0);
        }
        panel(g, px);
        ui.end();
    }

    /** A plain sample scene (sky, grass, stone, snow) left of the panel, with the crosshair in the real centre. */
    private void scene(Gfx g, int right) {
        int bandH = Math.max(1, height / SCENE.length);
        for (int i = 0; i < SCENE.length; i++) {
            int y = i * bandH;
            g.rect(0, y, right, i == SCENE.length - 1 ? height - y : bandH, SCENE[i]);
        }
        Crosshair.draw(g, m.crosshairStyle(style), Crosshair.centre(width), Crosshair.centre(height), 0);
    }

    private void panel(Gfx g, int x) {
        g.rect(x, 0, PANEL_W, height, Theme.alpha(Theme.PANEL, 0.94f));
        g.vline(x, 0, height, Theme.LINE2);
        int cx = x + PAD, cw = PANEL_W - PAD * 2;
        int y = PAD;
        g.bold(Lang.tr("pvp.editor.title"), cx, y, Theme.TEXT);
        y += 14;
        String note = crosshairBlocked() ? Lang.tr("core.settings.serverDisabled")
                : !m.isEnabled() ? Lang.tr("pvp.editor.moduleOff") : Lang.tr("pvp.editor.hint");
        int noteColor = crosshairBlocked() || !m.isEnabled() ? Theme.WARN : Theme.DIM;
        for (String line : Ui.wrap(g, note, cw)) {
            g.text(line, cx, y, noteColor);
            y += 10;
        }
        y += 4;
        y = magnified(g, cx, y, cw) + 8;

        int footerH = 18 + PAD * 2;
        int top = scroll.begin(ui, x + 1, y, PANEL_W - 1, height - y - footerH);
        int ry = top;
        ry = shapes(g, cx, ry, cw) + 6;
        boolean custom = m.crosshair.get() != Crosshair.Shape.VANILLA;
        if (custom) {
            ry = slider(g, m.chSize, cx, ry, cw);
            if (m.crosshair.get() != Crosshair.Shape.DOT) {
                ry = slider(g, m.chGap, cx, ry, cw);
                ry = slider(g, m.chThickness, cx, ry, cw);
            }
            ry = slider(g, m.chOpacity, cx, ry, cw);
            if (!m.chInvert.on()) {
                g.text(Lang.tr(m.chColor.nameKey()), cx, ry + 7, Theme.TEXT);
                int[] presets = m.chColor.presets();
                int sw = Widgets.swatchesWidth(presets.length);
                int picked = Widgets.swatches(ui, cx + cw - sw, ry + 5, presets, m.chColor.argb());
                if (picked != 0) m.chColor.set(picked);
                ry += ROW;
            }
            ry = toggle(m.chOutline, cx, ry, cw, !m.chInvert.on());
            if (m.crosshair.get() != Crosshair.Shape.DOT) ry = toggle(m.chDot, cx, ry, cw, true);
            ry = toggle(m.chDynamic, cx, ry, cw, true);
            ry = toggle(m.chInvert, cx, ry, cw, true);
        } else {
            for (String line : Ui.wrap(g, Lang.tr("pvp.editor.vanilla"), cw)) {
                g.text(line, cx, ry + 2, Theme.MUTED);
                ry += 10;
            }
        }
        scroll.end(ui, ry + 4);

        int by = height - PAD - 18;
        g.hline(x + 1, by - PAD, PANEL_W - 1, Theme.LINE);
        int bw = (cw - 6) / 2;
        if (Widgets.button(ui, RESET, cx, by, bw, 18, Lang.tr("pvp.editor.reset"), Widgets.Style.SECONDARY)) reset();
        if (Widgets.button(ui, DONE, cx + bw + 6, by, cw - bw - 6, 18, Lang.tr("pvp.editor.done"), Widgets.Style.PRIMARY)) onClose();
    }

    /** The crosshair 4× bigger over the four sample backgrounds, crisp (integer scale). */
    private int magnified(Gfx g, int x, int y, int w) {
        int h = 72;
        g.rect(x, y, w, h, Theme.SURFACE);
        int qw = w / 2, qh = h / 2;
        for (int i = 0; i < 4; i++) {
            int qx = x + (i % 2) * qw, qy = y + (i / 2) * qh;
            g.rect(qx, qy, i % 2 == 1 ? w - qw : qw, i / 2 == 1 ? h - qh : qh, SCENE[i]);
        }
        g.outline(x - 1, y - 1, w + 2, h + 2, Theme.LINE2);
        g.scissor(x, y, x + w, y + h);
        g.push();
        g.translate(x + w / 2 - MAG / 2, y + h / 2 - MAG / 2);
        g.scale(MAG, MAG);
        Crosshair.draw(g, m.crosshairStyle(style), 0, 0, 0);
        g.pop();
        g.endScissor();
        Widgets.chip(g, MAG + "×", x + 3, y + 3, Theme.TEXT);
        return y + h;
    }

    /** One tile per shape (Vanilla, Cross, T, Circle, Square, Dot), the selected one with a brand frame. */
    private int shapes(Gfx g, int x, int y, int w) {
        Widgets.caption(g, Lang.tr(m.crosshair.nameKey()), x, y);
        y += 11;
        Crosshair.Shape[] all = Crosshair.Shape.values();
        int cols = 3, gap = 4;
        int tw = (w - gap * (cols - 1)) / cols, th = 30;
        for (int i = 0; i < all.length; i++) {
            int tx = x + (i % cols) * (tw + gap), ty = y + (i / cols) * (th + gap);
            boolean sel = m.crosshair.get() == all[i];
            boolean clicked = ui.clicked(tx, ty, tw, th);
            boolean hot = ui.focusedLast() || ui.hover(tx, ty, tw, th);
            float hov = ui.anim(shapeKeys[i], 0f).to(hot ? 1f : 0f);
            g.rect(tx, ty, tw, th, sel ? Theme.alpha(Theme.BRAND, 0.18f) : Theme.mix(Theme.SURFACE2, Theme.SURFACE3, hov));
            g.outline(tx, ty, tw, th, sel ? Theme.BRAND : Theme.LINE2);
            m.crosshairStyle(tile);
            tile.shape = all[i];
            tile.size = Math.min(tile.size, 5);
            tile.gap = Math.min(tile.gap, 2);
            tile.thickness = 1;
            if (all[i] == Crosshair.Shape.DOT) tile.size = 5;
            Crosshair.draw(g, tile, tx + tw / 2, ty + 11, 0);
            String label = Lang.tr(m.crosshair.valueKey(all[i]));
            g.textCenter(g.clip(label, tw - 4), tx + tw / 2, ty + th - 9, sel ? Theme.TEXT : Theme.MUTED);
            if (clicked) m.crosshair.set(all[i]);
        }
        int rows = (all.length + cols - 1) / cols;
        return y + rows * (th + gap) - gap;
    }

    private int slider(Gfx g, IntSetting s, int x, int y, int w) {
        String value = s.get() + s.unit();
        g.text(Lang.tr(s.nameKey()), x, y + 7, Theme.TEXT);
        int sw = 84;
        g.textRight(value, x + w, y + 7, Theme.MUTED);
        float f = (s.get() - s.min()) / (float) Math.max(1, s.max() - s.min());
        float nf = Widgets.slider(ui, s, x + w - sw - 30, y + 5, sw, f);
        if (nf != f) s.set(s.min() + Math.round(nf * (s.max() - s.min()) / s.step()) * s.step());
        return y + ROW;
    }

    private int toggle(BoolSetting b, int x, int y, int w, boolean enabled) {
        boolean clicked = enabled && ui.clicked(x - 4, y, w + 8, ROW - 2);
        boolean hot = enabled && (ui.focusedLast() || ui.hover(x - 4, y, w + 8, ROW - 2));
        g().text(g().clip(Lang.tr(b.nameKey()), w - Widgets.TOGGLE_W - 8), x, y + 7, enabled ? Theme.TEXT : Theme.DIM);
        Widgets.drawToggle(ui, b, x + w - Widgets.TOGGLE_W, y + 5, b.on(), enabled, hot);
        if (clicked) b.toggle();
        return y + ROW;
    }

    private Gfx g() {
        return ui.g;
    }

    private void reset() {
        for (Setting<?, ?> s : new Setting<?, ?>[] {m.chSize, m.chGap, m.chThickness, m.chColor, m.chOpacity, m.chOutline,
                m.chDot, m.chDynamic, m.chInvert}) s.reset();
    }

    @Override
    protected boolean click(double x, double y, int button) {
        return ui.queueClick(x, y, button);
    }

    @Override
    protected boolean release(double x, double y, int button) {
        ui.release();
        return false;
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        ui.queueScroll(amount);
        return true;
    }

    @Override
    protected boolean key(int keyCode) {
        if (keyCode == Keys.ESCAPE) {
            onClose();
            return true;
        }
        return ui.key(keyCode, ClientCompat.shiftDown(), false);
    }

    @Override
    public void onClose() {
        ClientCompat.openScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean crosshairBlocked() {
        return ServerRules.isDisabled(PvpModule.RULE_CROSSHAIR);
    }
}
