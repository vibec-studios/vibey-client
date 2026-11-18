package dev.henny.vibeyclient.module.cosmetics.ui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.avatar.AvatarItem;
import dev.henny.vibeyclient.avatar.AvatarItems;
import dev.henny.vibeyclient.avatar.Wardrobe;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.Anim;
import dev.henny.vibeyclient.ui.BaseScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Widgets;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Editor for the 8 wheel slots: the wheel on the left (click a slot to select it, right click clears it), the emotes
 * on the right (click one to put it on the selected slot; an emote already on another slot swaps places).
 */
final class WheelEditorScreen extends BaseScreen {
    private static final int HEADER = ScreenUi.HEADER;
    private static final int GAP = 6;
    private static final String[] SLOT_NUMBERS = {"1", "2", "3", "4", "5", "6", "7", "8"};
    private static final Object CLOSE = new Object();
    private static final Object CLEAR = new Object();
    private static final Object DONE = new Object();

    private final Screen parent;
    private final ScreenUi ui = new ScreenUi();
    private final ItemIndex index = new ItemIndex();
    private final WheelLayout wheel = new WheelLayout();
    private final Scroller list = new Scroller();
    private final Anim open = new Anim(0f);
    private final Anim[] tiles = new Anim[WheelLayout.SLOTS];
    private final String[] ids = new String[WheelLayout.SLOTS];
    private final TextCache assignedText = new TextCache();
    private final TextCache slotText = new TextCache();
    private final TextCache hintText = new TextCache();
    private int selected = -1;
    private boolean closeRequested;

    WheelEditorScreen(Screen parent) {
        super(Component.literal("Emote wheel"));
        this.parent = parent;
        for (int i = 0; i < tiles.length; i++) tiles[i] = new Anim(0f);
    }

    /** Selected slot (0..7) or -1. */
    int selected() {
        return selected;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    protected void draw(Gfx g, int mx, int my, float delta) {
        ui.begin(g, mx, my, delta);
        Wardrobe wd = Services.get(Wardrobe.class).orElse(null);
        index.refresh("");
        int assigned = 0;
        for (int i = 0; i < ids.length; i++) {
            ids[i] = wd == null ? null : wd.wheel(i);
            if (ids[i] != null) assigned++;
        }
        if (selected < 0 && wd != null) selected = nextEmpty(-1);
        float o = open.to(1f);

        g.rect(0, 0, width, height, Theme.BG);
        ui.header(width, Lang.tr("wardrobe.wheel.title"), wd == null ? null : assignedText.tr("wardrobe.wheel.assigned", assigned));
        if (ui.closeButton(CLOSE, width - 24, (HEADER - 18) / 2)) closeRequested = true;

        int y0 = HEADER + 1;
        int bodyH = height - y0;
        int leftW = clamp(width * 2 / 5, 150, 380);
        g.rect(0, y0, leftW, bodyH, Theme.PANEL);
        g.vline(leftW, y0, bodyH, Theme.LINE);
        g.rect(leftW + 1, y0, width - leftW - 1, bodyH, Theme.SURFACE);

        AvatarItem hoveredEmote = drawList(g, wd, leftW + 1, y0, width - leftW - 1, bodyH);
        drawWheel(g, wd, hoveredEmote, 0, y0, leftW, bodyH);

        if (o < 1f) g.rect(0, y0, width, height, Theme.alpha(Theme.BG, 1f - o));
        ui.end();
        if (closeRequested) {
            closeRequested = false;
            onClose();
        }
    }

    private int nextEmpty(int after) {
        for (int k = 1; k <= ids.length; k++) {
            int i = Math.floorMod(after + k, ids.length);
            if (ids[i] == null) return i;
        }
        return after < 0 ? 0 : after;
    }

    // ============================================================================================ left: the wheel

    private void drawWheel(Gfx g, Wardrobe wd, AvatarItem hoveredEmote, int x, int y, int w, int h) {
        int pad = Theme.PAD;
        int bottom = 10 + 10 + 8 + 18 + pad; // two hint lines, gap, buttons
        int avail = Math.min(w - pad * 2, h - bottom - pad * 2);
        int gap = 4;
        int tile = clamp((avail - gap * 2) / 3, 26, 64);
        wheel.set(x + w / 2, y + pad + (h - bottom - pad) / 2, tile, gap);

        for (int i = 0; i < WheelLayout.SLOTS; i++) {
            int tx = wheel.tileX(i), ty = wheel.tileY(i);
            if (wd != null) {
                if (ui.clicked(tx, ty, tile, tile)) selected = i;
                if (ui.clicked(tx, ty, tile, tile, 1)) {
                    wd.setWheel(i, null);
                    ids[i] = null;
                    selected = i;
                }
            }
            float t = tiles[i].to(ui.hover(tx, ty, tile, tile) ? 1f : 0f);
            WheelLayout.drawTile(g, tx, ty, tile, ids[i], t, i == selected, 1f);
            if (tile >= 34) g.text(SLOT_NUMBERS[i], tx + 3, ty + 3, i == selected ? Theme.BRAND : Theme.DIM);
        }

        // centre: the selected slot (or the emote under the mouse)
        int cx = wheel.centreX(), cy = wheel.centreY();
        g.rect(cx, cy, tile, tile, Theme.SURFACE);
        g.outline(cx, cy, tile, tile, Theme.LINE);
        int tw = tile - 6;
        if (wd != null && selected >= 0) {
            String slot = slotText.tr("wardrobe.wheel.slot", selected + 1);
            String name;
            if (hoveredEmote != null) name = hoveredEmote.name();
            else if (ids[selected] != null) {
                AvatarItem item = AvatarItems.get(ids[selected]);
                name = item != null ? item.name() : ids[selected];
            } else name = Lang.tr("wardrobe.wheel.empty");
            g.textCenter(g.clip(slot, tw), cx + tile / 2, cy + tile / 2 - 9, Theme.BRAND);
            String n = g.clip(name, tw);
            g.bold(n, cx + tile / 2 - g.boldWidth(n) / 2, cy + tile / 2 + 2, hoveredEmote != null ? Theme.BRAND2 : Theme.TEXT);
        }

        int hy = wheel.y + wheel.size() + 10;
        if (wd == null) {
            g.textCenter(g.clip(Lang.tr("wardrobe.wheel.unavailable"), w - pad * 2), x + w / 2, hy, Theme.WARN);
        } else {
            g.textCenter(g.clip(Lang.tr("wardrobe.wheel.editorHint1"), w - pad * 2), x + w / 2, hy, Theme.MUTED);
            g.textCenter(g.clip(Lang.tr("wardrobe.wheel.editorHint2"), w - pad * 2), x + w / 2, hy + 10, Theme.DIM);
        }
        int by = y + h - pad - 18;
        int bw = (w - pad * 2 - GAP) / 2;
        if (ui.button(CLEAR, x + pad, by, bw, 18, Lang.tr("wardrobe.wheel.clear"), wd == null ? Widgets.Style.DISABLED : Widgets.Style.DANGER)) {
            for (int i = 0; i < ids.length; i++) wd.setWheel(i, null);
            selected = 0;
        }
        if (ui.button(DONE, x + pad + bw + GAP, by, w - pad * 2 - bw - GAP, 18, Lang.tr("wardrobe.wheel.done"), Widgets.Style.PRIMARY)) {
            closeRequested = true;
        }
    }

    // ============================================================================================ right: emotes

    private AvatarItem drawList(Gfx g, Wardrobe wd, int x, int y, int w, int h) {
        int pad = Theme.PAD;
        String caption = Category.EMOTES.label();
        g.bold(g.clip(caption, w - pad * 2), x + pad, y + 10, Theme.TEXT);
        String count = index.countText(Category.EMOTES);
        int lx = x + pad + g.boldWidth(caption) + 6;
        if (lx + g.width(count) < x + w - pad) g.text(count, lx, y + 10, Theme.DIM);

        List<AvatarItem> items = index.items(Category.EMOTES);
        int ay = y + 28;
        int ah = y + h - ay;
        int ax = x + pad;
        int aw = w - pad * 2 + 4;
        if (items.isEmpty()) {
            int cx = x + w / 2;
            int cy = ay + ah / 2 - 30;
            g.outline(cx - 12, cy, 24, 24, Theme.LINE2);
            Cards.plus(g, cx, cy + 12, 8, Theme.DIM);
            String t = g.clip(Lang.tr("wardrobe.empty.emotes"), w - pad * 4);
            g.bold(t, cx - g.boldWidth(t) / 2, cy + 34, Theme.MUTED);
            int ly = cy + 48;
            for (String line : hintText.wrap(g, Lang.tr("wardrobe.empty.categoryEmotes"), w - pad * 4)) {
                g.textCenter(line, cx, ly, Theme.DIM);
                ly += 10;
            }
            return null;
        }
        int innerW = aw - 8;
        int cols = Math.max(1, (innerW + GAP) / (60 + GAP));
        int cardW = (innerW - GAP * (cols - 1)) / cols;
        if (cardW > 92) {
            cols = (innerW + GAP + 92 + GAP - 1) / (92 + GAP);
            cardW = (innerW - GAP * (cols - 1)) / cols;
        }
        int cardH = Cards.height(cardW);
        AvatarItem hovered = null;
        int top = list.begin(ui, ax - 1, ay, aw + 1, ah) + 1;
        for (int i = 0; i < items.size(); i++) {
            AvatarItem item = items.get(i);
            int cx = ax + (i % cols) * (cardW + GAP);
            int cy = top + (i / cols) * (cardH + GAP);
            if (!ui.visibleY(cy, cardH)) continue;
            boolean hov = ui.hover(cx, cy, cardW, cardH);
            boolean usable = wd != null && wd.canUse(item);
            if (hov && usable) hovered = item;
            if (usable && selected >= 0 && ui.clicked(cx, cy, cardW, cardH)) assign(wd, item.id());
            int slot = -1;
            for (int k = 0; k < ids.length; k++) if (item.id().equals(ids[k])) slot = k;
            int flags = (slot >= 0 && slot == selected ? Cards.SELECTED : 0) | (wd != null && !usable ? Cards.LOCKED : 0);
            String reason = null;
            if (wd != null && !usable) {
                reason = wd.lockedReason(item);
                if (reason == null) reason = Lang.tr("wardrobe.locked");
            }
            float t = ui.anim(item.id(), 0, 0f).to(hov ? 1f : 0f);
            Cards.draw(g, item, cx, cy, cardW, flags, t, slot >= 0 ? SLOT_NUMBERS[slot] : null, reason);
        }
        int rows = (items.size() + cols - 1) / cols;
        list.end(ui, top + rows * (cardH + GAP) - GAP + 2);
        return hovered;
    }

    private void assign(Wardrobe wd, String emoteId) {
        int sel = selected;
        String previous = ids[sel];
        for (int k = 0; k < ids.length; k++) {
            if (k != sel && emoteId.equals(ids[k])) {
                wd.setWheel(k, previous); // swap places
                ids[k] = previous;
            }
        }
        wd.setWheel(sel, emoteId);
        ids[sel] = emoteId;
        selected = nextEmpty(sel);
    }

    // ============================================================================================ input

    @Override
    protected boolean click(double x, double y, int button) {
        return ui.queueClick(x, y, button);
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        ui.queueScroll(amount);
        return true;
    }

    @Override
    public void onClose() {
        ClientCompat.openScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return parent != null && parent.isPauseScreen();
    }
}
