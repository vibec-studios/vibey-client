package dev.henny.vibeyclient.module.cosmetics.ui;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.VibeBackend;
import dev.henny.vibeyclient.avatar.AvatarItem;
import dev.henny.vibeyclient.avatar.AvatarItems;
import dev.henny.vibeyclient.avatar.Wardrobe;
import dev.henny.vibeyclient.avatar.preview.AvatarPreview;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.Anim;
import dev.henny.vibeyclient.ui.BaseScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Widgets;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The full-screen wardrobe: categories on the left (with counts), a grid of item cards in the middle, the 3D preview
 * of the player on the right. Clicking a cosmetic equips/unequips it, hovering tries it on in the preview, clicking an
 * emote selects (and previews) it. Nothing here is purchasable: rarity is only a coloured label.
 */
final class WardrobeScreen extends BaseScreen {
    private static final int HEADER = ScreenUi.HEADER;
    private static final int GAP = 6;
    private static final int MIN_CARD = 70;
    private static final int MAX_CARD = 104;
    private static final String[] SLOT_NUMBERS = {"1", "2", "3", "4", "5", "6", "7", "8"};

    private static final Object CLOSE = new Object();
    private static final Object WHEEL = new Object();
    private static final Object EDIT_WHEEL = new Object();
    private static final Object PLAY = new Object();
    private static final Object RESET = new Object();
    private static final Object CLEAR_SEARCH = new Object();

    /** Remembered across openings. */
    private static Category lastCategory = Category.CAPE;

    private final Screen parent;
    private final ScreenUi ui = new ScreenUi();
    private final ItemIndex index = new ItemIndex();
    private final PreviewHost preview = new PreviewHost();
    private final Scroller grid = new Scroller();
    private final Anim open = new Anim(0f);
    private final Anim gridIn = new Anim(1f);
    private final String[] wheelIds = new String[Wardrobe.WHEEL_SLOTS];
    private final TextCache itemsText = new TextCache();
    private final TextCache emptyText = new TextCache();
    private final TextCache hintText = new TextCache();
    private final TextCache bannerText = new TextCache();

    private Category category;
    private String selectedEmote;
    private EditBox search;
    private String query = "";
    private int searchX, searchY, searchW;
    private boolean dragging;
    private boolean closeRequested;
    /** Clicked item: no try-on override for it until the mouse leaves the card (so unequip shows at once). */
    private String suppressTryOn;

    WardrobeScreen(Screen parent, Category initial) {
        super(Component.literal("Wardrobe"));
        this.parent = parent;
        this.category = initial != null ? initial : lastCategory;
        lastCategory = this.category;
        if (this.category != null) preview.faceFor(this.category.slot);
    }

    @Override
    protected void init() {
        if (search == null) {
            search = new EditBox(font, 0, 0, 100, 12, Component.empty());
            search.setBordered(false);
            search.setMaxLength(48);
            search.setTextColor(Theme.TEXT);
            search.setResponder(s -> {
                query = s;
                grid.reset();
            });
        }
        addWidget(search);
    }

    // ============================================================================================ layout

    private int listWidth() {
        return clamp(width / 6, 84, 140);
    }

    private int previewWidth() {
        return clamp(width * 3 / 10, 110, 300);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    protected void draw(Gfx g, int mx, int my, float delta) {
        ui.begin(g, mx, my, delta);
        Wardrobe wd = Services.get(Wardrobe.class).orElse(null);
        index.refresh(query);
        for (int i = 0; i < wheelIds.length; i++) wheelIds[i] = wd == null ? null : wd.wheel(i);
        if (selectedEmote != null && AvatarItems.get(selectedEmote) == null) selectedEmote = null;
        float o = open.to(1f);

        g.rect(0, 0, width, height, Theme.BG);
        int hx = ui.header(width, Lang.tr("wardrobe.title"), itemsText.tr("wardrobe.items", index.total()));
        int closeX = width - 24;
        if (ui.closeButton(CLOSE, closeX, (HEADER - 18) / 2)) closeRequested = true;
        int sw = Math.min(clamp(width / 4, 80, 180), closeX - 8 - hx);
        drawSearch(g, closeX - 8 - sw, (HEADER - 16) / 2, sw);

        int y0 = HEADER + 1;
        int bodyH = height - y0;
        int listW = listWidth();
        int prevW = previewWidth();
        int gridX = listW + 1;
        int gridW = width - listW - 1 - prevW - 1;

        g.rect(0, y0, width, bodyH, Theme.SURFACE);
        drawCategories(g, 0, y0, listW, bodyH);
        AvatarItem hovered = drawGrid(g, wd, gridX, y0, gridW, bodyH);
        drawPreviewColumn(g, wd, hovered, width - prevW, y0, prevW, bodyH);

        // fade in from black, everything below the header (cheap overlay, input works during it)
        if (o < 1f) g.rect(0, y0, width, height, Theme.alpha(Theme.BG, 1f - o));
        ui.end();
        if (closeRequested) {
            closeRequested = false;
            onClose();
        }
    }

    private void drawSearch(Gfx g, int x, int y, int w) {
        searchX = x;
        searchY = y;
        searchW = w;
        if (w < 40) {
            search.visible = false;
            return;
        }
        boolean focused = search.isFocused();
        float f = ui.anim(search, 4, 0f).to(focused ? 1f : 0f);
        g.rect(x, y, w, 16, Theme.mix(Theme.SURFACE2, Theme.SURFACE, f));
        g.outline(x, y, w, 16, Theme.mix(Theme.LINE2, Theme.BRAND, f));
        magnifier(g, x + 5, y + 4, focused ? Theme.TEXT : Theme.DIM);
        boolean hasText = !search.getValue().isEmpty();
        int clearW = hasText ? 14 : 0;
        search.setX(x + 17);
        search.setY(y + 4);
        search.setWidth(w - 22 - clearW);
        search.visible = true;
        g.widget(search, ui.mouseX, ui.mouseY, ui.delta);
        if (!hasText && !focused) g.text(g.clip(Lang.tr("wardrobe.search"), w - 22), x + 17, y + 4, Theme.DIM);
        if (hasText) {
            int cx = x + w - 14;
            boolean h = ui.hover(cx, y, 14, 16);
            if (ui.clicked(cx, y, 14, 16)) search.setValue("");
            Widgets.cross(g, cx + 4, y + 5, 6, h ? Theme.TEXT : Theme.DIM);
        }
    }

    private static void magnifier(Gfx g, int x, int y, int c) {
        g.outline(x, y, 6, 6, c);
        g.rect(x + 6, y + 6, 1, 1, c);
        g.rect(x + 7, y + 7, 1, 1, c);
    }

    // ============================================================================================ left: categories

    private void drawCategories(Gfx g, int x, int y, int w, int h) {
        g.rect(x, y, w, h, Theme.PANEL);
        g.vline(x + w, y, h, Theme.LINE);
        Widgets.caption(g, Lang.tr("wardrobe.categories"), x + Theme.PAD, y + 8);
        int footer = 18 + Theme.PAD * 2;
        int top = y + 20;
        int rowH = clamp((h - 20 - footer) / Category.ALL.length, 14, 22);
        for (Category c : Category.ALL) {
            boolean sel = c == category;
            if (ui.clicked(x, top, w, rowH)) select(c);
            float hov = ui.anim(c, 0, 0f).to(sel ? 1f : ui.hover(x, top, w, rowH) ? 0.6f : 0f);
            float s = ui.anim(c, 5, sel ? 1f : 0f).to(sel ? 1f : 0f);
            if (hov > 0f) g.rect(x, top, w, rowH, Theme.alpha(Theme.SURFACE3, hov));
            if (s > 0f) g.rect(x, top + Math.round(rowH * (1f - s) / 2f), 2, Math.max(1, Math.round(rowH * s)), Theme.BRAND);
            String count = index.countText(c);
            int cw = g.width(count);
            int ty = top + (rowH - 8) / 2;
            g.textRight(count, x + w - Theme.PAD, ty, sel ? Theme.BRAND : Theme.DIM);
            g.text(g.clip(c.label(), w - Theme.PAD * 2 - cw - 4), x + Theme.PAD, ty, sel ? Theme.TEXT : Theme.mix(Theme.MUTED, Theme.TEXT, hov));
            top += rowH;
        }
        int by = y + h - Theme.PAD - 18;
        if (by > top + 4 && ui.button(WHEEL, x + 6, by, w - 12, 18, Lang.tr("wardrobe.wheel.title"), Widgets.Style.SECONDARY)) {
            WardrobeUi.openWheelEditor(this);
        }
    }

    private void select(Category c) {
        if (c == category) return;
        category = c;
        lastCategory = c;
        grid.reset();
        gridIn.snap(0f);
        preview.tryOn(null, null);
        preview.faceFor(c.slot);
    }

    // ============================================================================================ middle: grid

    /** Draws the toolbar and the cards; returns the hovered item (or null). */
    private AvatarItem drawGrid(Gfx g, Wardrobe wd, int x, int y, int w, int h) {
        int pad = Theme.PAD;
        int ty = y + 10;
        String label = category.label();
        g.bold(g.clip(label, w - pad * 2), x + pad, ty, Theme.TEXT);
        String count = index.countText(category);
        int lx = x + pad + g.boldWidth(label) + 6;
        int toolbarRight = x + w - pad;
        if (category.emotes()) {
            String edit = Lang.tr("wardrobe.wheel.edit");
            int bw = g.width(edit) + 16;
            if (lx + g.width(count) + 8 + bw <= toolbarRight) {
                if (ui.button(EDIT_WHEEL, toolbarRight - bw, y + 6, bw, 16, edit, Widgets.Style.SECONDARY)) WardrobeUi.openWheelEditor(this);
                toolbarRight -= bw + 8;
            }
        }
        if (lx + g.width(count) <= toolbarRight) g.text(count, lx, ty, Theme.DIM);

        int ay = y + 28;
        if (wd == null) {
            String msg = Lang.tr(category.emotes() ? "wardrobe.wheel.unavailable" : "wardrobe.unavailable");
            List<String> lines = bannerText.wrap(g, msg, w - pad * 2 - 12);
            int bh = lines.size() * 10 + 8;
            g.rect(x + pad, ay, w - pad * 2, bh, Theme.alpha(Theme.WARN, 0.10f));
            g.vline(x + pad, ay, bh, Theme.WARN);
            for (int i = 0; i < lines.size(); i++) g.text(lines.get(i), x + pad + 8, ay + 5 + i * 10, Theme.WARN);
            ay += bh + 6;
        }
        int ax = x + pad;
        int aw = w - pad * 2 + 4; // the scrollbar sits in the right padding
        int ah = y + h - ay;
        List<AvatarItem> items = index.items(category);
        if (items.isEmpty()) {
            preview.tryOn(null, null);
            drawEmpty(g, x, ay, w, ah);
            return null;
        }

        int innerW = aw - 4 - 4;
        int cols = Math.max(1, (innerW + GAP) / (MIN_CARD + GAP));
        int cardW = (innerW - GAP * (cols - 1)) / cols;
        if (cardW > MAX_CARD) {
            cols = (innerW + GAP + MAX_CARD + GAP - 1) / (MAX_CARD + GAP);
            cardW = (innerW - GAP * (cols - 1)) / cols;
        }
        int cardH = Cards.height(cardW);
        float a = gridIn.to(1f);
        int slide = Math.round((1f - a) * 6f);

        AvatarItem hovered = null;
        int top = grid.begin(ui, ax - 1, ay, aw + 1, ah);
        int cy = top + 1 + slide;
        for (int i = 0; i < items.size(); i++) {
            AvatarItem item = items.get(i);
            int cx = ax + (i % cols) * (cardW + GAP);
            int ry = cy + (i / cols) * (cardH + GAP);
            if (!ui.visibleY(ry, cardH)) continue;
            boolean hov = ui.hover(cx, ry, cardW, cardH);
            if (hov) hovered = item;
            if (ui.clicked(cx, ry, cardW, cardH)) onCard(wd, item);
            int flags = 0;
            String reason = null;
            String badge = null;
            if (item.isEmote()) {
                if (item.id().equals(selectedEmote)) flags |= Cards.SELECTED;
                badge = wheelSlotOf(item.id());
            } else if (wd != null && item.id().equals(wd.equipped(item.slot()))) {
                flags |= Cards.EQUIPPED;
            }
            if (wd != null && !wd.canUse(item)) {
                flags |= Cards.LOCKED;
                reason = wd.lockedReason(item);
                if (reason == null) reason = Lang.tr("wardrobe.locked");
            }
            float t = ui.anim(item.id(), 0, 0f).to(hov ? 1f : 0f);
            Cards.draw(g, item, cx, ry, cardW, flags, t, badge, reason);
        }
        int rows = (items.size() + cols - 1) / cols;
        int end = cy + rows * (cardH + GAP) - GAP + 2;
        if (a < 1f) g.rect(ax - 1, ay, aw + 1, ah, Theme.alpha(Theme.SURFACE, (1f - a) * 0.8f));
        grid.end(ui, end - slide);

        // hover try-on (cosmetics only)
        if (hovered == null || !hovered.id().equals(suppressTryOn)) suppressTryOn = null;
        if (hovered != null && !hovered.isEmote() && suppressTryOn == null) preview.tryOn(hovered.slot(), hovered.id());
        else preview.tryOn(null, null);
        return hovered;
    }

    private String wheelSlotOf(String id) {
        for (int i = 0; i < wheelIds.length; i++) if (id.equals(wheelIds[i])) return SLOT_NUMBERS[i];
        return null;
    }

    private void onCard(Wardrobe wd, AvatarItem item) {
        if (item.isEmote()) {
            selectedEmote = item.id();
            preview.playEmote(item.id());
            return;
        }
        if (wd == null || !wd.canUse(item)) return;
        if (item.id().equals(wd.equipped(item.slot()))) wd.unequip(item.slot());
        else wd.equip(item);
        suppressTryOn = item.id();
        preview.tryOn(null, null);
    }

    private void drawEmpty(Gfx g, int x, int y, int w, int h) {
        int pad = Theme.PAD;
        String title;
        String hint;
        if (index.searching()) {
            title = emptyText.tr("wardrobe.empty.search", index.query().trim());
            hint = Lang.tr("wardrobe.empty.searchHint");
        } else if (index.total() == 0) {
            title = Lang.tr("wardrobe.empty.title");
            if (!GlobalSettings.online()) hint = Lang.tr("wardrobe.empty.offline");
            else if (Services.get(VibeBackend.class).map(VibeBackend::isReady).orElse(false)) hint = Lang.tr("wardrobe.empty.hint");
            else hint = Lang.tr("wardrobe.empty.disconnected");
        } else {
            title = Lang.tr("wardrobe.empty.category");
            hint = Lang.tr(category.emotes() ? "wardrobe.empty.categoryEmotes" : "wardrobe.empty.categoryHint");
        }
        int tw = w - pad * 4;
        List<String> lines = hintText.wrap(g, hint, tw);
        int blockH = 24 + 10 + 14 + lines.size() * 10;
        int cy = y + Math.max(8, (h - blockH) / 2 - 10);
        int cx = x + w / 2;
        g.outline(cx - 12, cy, 24, 24, Theme.LINE2);
        Cards.plus(g, cx, cy + 12, 8, Theme.DIM);
        cy += 34;
        String t = g.clip(title, tw);
        g.bold(t, cx - g.boldWidth(t) / 2, cy, Theme.MUTED);
        cy += 14;
        for (String line : lines) {
            g.textCenter(line, cx, cy, Theme.DIM);
            cy += 10;
        }
    }

    // ============================================================================================ right: preview

    private void drawPreviewColumn(Gfx g, Wardrobe wd, AvatarItem hovered, int x, int y, int w, int h) {
        int pad = Theme.PAD;
        g.rect(x, y, w, h, Theme.PANEL);
        g.vline(x - 1, y, h, Theme.LINE);

        // what we are looking at: hovered card, else the selected emote / the item equipped in this category
        AvatarItem info = hovered;
        if (info == null) {
            if (category.emotes()) info = AvatarItems.get(selectedEmote);
            else if (wd != null) info = AvatarItems.get(wd.equipped(category.slot));
        }
        int iw = w - pad * 2;
        if (info != null) {
            g.bold(g.clip(info.name(), iw), x + pad, y + 8, Theme.TEXT);
            String rarity = Cards.rarityLabel(info.rarity());
            int rx = x + pad;
            g.text(g.clip(rarity, iw), rx, y + 20, info.rarity().color);
            rx += g.width(rarity) + 6;
            String state = null;
            int stateColor = Theme.DIM;
            if (wd != null && !wd.canUse(info)) {
                state = wd.lockedReason(info);
                if (state == null) state = Lang.tr("wardrobe.locked");
                stateColor = Theme.WARN;
            } else if (!info.isEmote() && wd != null && info.id().equals(wd.equipped(info.slot()))) {
                state = Lang.tr("wardrobe.equipped");
                stateColor = Theme.BRAND;
            } else if (info == hovered && !info.isEmote()) {
                state = Lang.tr("wardrobe.tryingOn");
                stateColor = Theme.BRAND2;
            }
            if (state != null && rx < x + w - pad - 12) g.text(g.clip(state, x + w - pad - rx), rx, y + 20, stateColor);
        } else {
            g.bold(g.clip(category.label(), iw), x + pad, y + 8, Theme.MUTED);
            g.text(g.clip(Lang.tr(category.emotes() ? "wardrobe.info.noEmote" : "wardrobe.info.none"), iw), x + pad, y + 20, Theme.DIM);
        }

        // buttons at the bottom
        boolean emotes = category.emotes();
        int bh = 18;
        boolean stacked = emotes && (iw - GAP) / 2 < 64;
        int buttonsH = stacked ? bh * 2 + GAP : bh;
        int by = y + h - pad - buttonsH;

        int vy = y + 34;
        int vh = by - pad - vy;
        int vx = x + pad;
        g.rect(vx, vy, iw, vh, Theme.BG);
        g.outline(vx, vy, iw, vh, Theme.LINE);
        if (vh > 20) {
            preview.draw(g, vx + 1, vy + 1, iw - 2, vh - 2, ui.delta);
            if (preview.available() && !dragging && ui.hover(vx, vy, iw, vh)) {
                g.textCenter(g.clip(Lang.tr("wardrobe.preview.hint"), iw - 8), vx + iw / 2, vy + vh - 12, Theme.DIM);
            }
        }

        if (emotes) {
            String playing = preview.emote();
            boolean canPlay = selectedEmote != null && preview.available();
            String label = Lang.tr(playing != null ? "wardrobe.preview.stop" : "wardrobe.preview.emote");
            Widgets.Style style = playing != null ? Widgets.Style.SECONDARY : canPlay ? Widgets.Style.PRIMARY : Widgets.Style.DISABLED;
            int pw = stacked ? iw : (iw - GAP) / 2;
            if (ui.button(PLAY, vx, by, pw, bh, label, style)) {
                if (playing != null) preview.stopEmote();
                else preview.playEmote(selectedEmote);
            }
            int rx = stacked ? vx : vx + pw + GAP;
            int ry = stacked ? by + bh + GAP : by;
            if (ui.button(RESET, rx, ry, stacked ? iw : iw - pw - GAP, bh, Lang.tr("wardrobe.preview.reset"),
                    preview.available() ? Widgets.Style.SECONDARY : Widgets.Style.DISABLED)) {
                preview.reset();
            }
        } else if (ui.button(RESET, vx, by, iw, bh, Lang.tr("wardrobe.preview.reset"),
                preview.available() ? Widgets.Style.SECONDARY : Widgets.Style.DISABLED)) {
            preview.reset();
        }
    }

    // ============================================================================================ input

    @Override
    protected boolean click(double x, double y, int button) {
        if (search != null && search.visible && x >= searchX && x < searchX + searchW - (search.getValue().isEmpty() ? 0 : 14)
                && y >= searchY && y < searchY + 16) {
            return false; // the text field takes it
        }
        if (search != null) search.setFocused(false);
        if (button == 0 && preview.mouse(AvatarPreview.MouseAction.PRESS, x, y)) {
            dragging = true;
            return true;
        }
        return ui.queueClick(x, y, button);
    }

    @Override
    protected boolean drag(double x, double y, int button) {
        if (dragging) {
            preview.mouse(AvatarPreview.MouseAction.DRAG, x, y);
            return true;
        }
        return false;
    }

    @Override
    protected boolean release(double x, double y, int button) {
        if (dragging) {
            dragging = false;
            preview.mouse(AvatarPreview.MouseAction.RELEASE, x, y);
            return true;
        }
        return false;
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        ui.queueScroll(amount);
        return true;
    }

    @Override
    public void onClose() {
        preview.tryOn(null, null);
        ClientCompat.openScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return parent != null && parent.isPauseScreen();
    }
}
