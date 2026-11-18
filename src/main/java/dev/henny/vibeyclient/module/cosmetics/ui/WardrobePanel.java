package dev.henny.vibeyclient.module.cosmetics.ui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.avatar.AvatarItem;
import dev.henny.vibeyclient.avatar.AvatarItems;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.avatar.Wardrobe;
import dev.henny.vibeyclient.avatar.preview.AvatarPreview;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Panel;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;

/**
 * The "Wardrobe" tab of the Vibey menu: a turning preview of the player on the left, the current loadout (click a
 * row to open the wardrobe there) and the emote wheel summary on the right, plus the buttons to open the full
 * wardrobe and the wheel editor.
 */
final class WardrobePanel implements Panel {
    private static final Object PREVIEW = new Object();
    private static final Object OPEN = new Object();
    private static final Object EDIT = new Object();

    private final PreviewHost preview = new PreviewHost().autoRotate(true);
    private final ScrollArea scroll = new ScrollArea();
    private final TextCache assignedText = new TextCache();
    private final TextCache descText = new TextCache();
    private final TextCache warnText = new TextCache();
    private boolean dragging;

    @Override
    public void draw(Ui ui, int x, int y, int w, int h) {
        Gfx g = ui.g;
        Wardrobe wd = Services.get(Wardrobe.class).orElse(null);
        int pad = Theme.PAD + 4;

        // left: preview
        int pw = Math.max(96, Math.min(220, w * 2 / 5));
        g.rect(x, y, pw, h, Theme.PANEL);
        g.vline(x + pw, y, h, Theme.LINE);
        int vx = x + Theme.PAD, vy = y + Theme.PAD, vw = pw - Theme.PAD * 2, vh = h - Theme.PAD * 2;
        g.rect(vx, vy, vw, vh, Theme.BG);
        g.outline(vx, vy, vw, vh, Theme.LINE);
        boolean drag = ui.dragging(PREVIEW, vx, vy, vw, vh);
        if (drag) {
            preview.mouse(dragging ? AvatarPreview.MouseAction.DRAG : AvatarPreview.MouseAction.PRESS, ui.mouseX, ui.mouseY);
        } else if (dragging) {
            preview.mouse(AvatarPreview.MouseAction.RELEASE, ui.mouseX, ui.mouseY);
        }
        dragging = drag;
        preview.draw(g, vx + 1, vy + 1, vw - 2, vh - 2, ui.delta);

        // right: text, loadout, wheel, buttons (scrolls on small windows)
        int rx = x + pw + 1;
        int rw = w - pw - 1;
        int cx = rx + pad;
        int cw = rw - pad * 2;
        int cy = scroll.begin(ui, rx, y, rw, h) + pad;

        g.bold(g.clip(Lang.tr("wardrobe.title"), cw), cx, cy, Theme.TEXT);
        cy += 13;
        for (String line : descText.wrap(g, Lang.tr("wardrobe.panel.description"), cw)) {
            g.text(line, cx, cy, Theme.MUTED);
            cy += 10;
        }
        cy += 8;

        int bw = Math.min(cw, 220);
        if (Widgets.button(ui, OPEN, cx, cy, bw, 20, Lang.tr("wardrobe.panel.open"), Widgets.Style.PRIMARY)) {
            WardrobeUi.openWardrobe(ClientCompat.screen());
        }
        cy += 28;

        Widgets.caption(g, Lang.tr("wardrobe.panel.loadout"), cx, cy);
        cy += 13;
        int rowH = 16;
        for (Slot slot : Slot.all()) {
            Category c = Category.of(slot);
            boolean hov = ui.hover(cx - 4, cy, cw + 8, rowH);
            if (ui.clicked(cx - 4, cy, cw + 8, rowH)) WardrobeUi.openWardrobe(ClientCompat.screen(), c);
            float t = ui.anim(c, 5, 0f).to(hov ? 1f : 0f);
            if (t > 0f) g.rect(cx - 4, cy, cw + 8, rowH, Theme.alpha(Theme.SURFACE3, t));
            String label = c.label();
            int lw = Math.min(g.width(label), cw / 2);
            g.text(g.clip(label, cw / 2), cx, cy + 4, Theme.mix(Theme.MUTED, Theme.TEXT, t));
            String id = wd == null ? null : wd.equipped(slot);
            AvatarItem item = AvatarItems.get(id);
            if (item != null) {
                String name = g.clip(item.name(), cw - lw - 18);
                int nx = cx + cw - g.width(name);
                g.text(name, nx, cy + 4, Theme.TEXT);
                g.rect(nx - 7, cy + 6, 3, 3, item.rarity().color);
            } else {
                g.textRight("—", cx + cw, cy + 4, Theme.DIM);
            }
            cy += rowH;
        }
        cy += 10;

        Widgets.caption(g, Lang.tr("wardrobe.wheel.title"), cx, cy);
        cy += 13;
        int assigned = 0;
        int sq = 10;
        for (int i = 0; i < Wardrobe.WHEEL_SLOTS; i++) {
            boolean on = wd != null && wd.wheel(i) != null;
            if (on) assigned++;
            int sx = cx + i * (sq + 3);
            g.rect(sx, cy, sq, sq, on ? Theme.alpha(Theme.BRAND, 0.85f) : Theme.SURFACE2);
            g.outline(sx, cy, sq, sq, on ? Theme.BRAND : Theme.LINE2);
        }
        int tx = cx + Wardrobe.WHEEL_SLOTS * (sq + 3) + 4;
        if (wd != null) g.text(g.clip(assignedText.tr("wardrobe.wheel.assigned", assigned), cx + cw - tx), tx, cy + 1, Theme.MUTED);
        cy += sq + 8;
        if (Widgets.button(ui, EDIT, cx, cy, bw, 18, Lang.tr("wardrobe.wheel.edit"), wd == null ? Widgets.Style.DISABLED : Widgets.Style.SECONDARY)) {
            WardrobeUi.openWheelEditor(ClientCompat.screen());
        }
        cy += 18;
        if (wd == null) {
            cy += 8;
            for (String line : warnText.wrap(g, Lang.tr("wardrobe.unavailable"), cw)) {
                g.text(line, cx, cy, Theme.WARN);
                cy += 10;
            }
        }
        scroll.end(ui, cy + pad);
    }

    @Override
    public void onHide() {
        if (dragging) {
            dragging = false;
            preview.mouse(AvatarPreview.MouseAction.RELEASE, 0, 0);
        }
    }
}
