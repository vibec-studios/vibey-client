package dev.henny.vibeyclient.module.cosmetics.ui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.model.Rarity;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.mc.thumbs.McThumbnails;
import dev.henny.vibeyclient.module.cosmetics.preview.AvatarPreview;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Widgets;

/**
 * Drawing of item cards and thumbnails shared by the wardrobe, the wheel editor and the emote wheel. Cards are square
 * thumbnails with the name and the rarity as a coloured text label below (no prices: rarity is only a label).
 */
final class Cards {
    private Cards() {}

    /** Card height for a card width (thumbnail square + two text lines). */
    static int height(int w) {
        return w + 22;
    }

    /** Card states (bit flags). */
    static final int EQUIPPED = 1, LOCKED = 2, SELECTED = 4;

    /**
     * Draws one card. {@code hov} is the 0..1 hover animation, {@code badge} an optional short text in the bottom
     * right of the thumbnail (wheel slot number), {@code lockedReason} replaces the rarity line of locked items.
     */
    static void draw(Gfx g, Item item, int x, int y, int w, int flags, float hov, String badge, String lockedReason) {
        int h = height(w);
        boolean equipped = (flags & EQUIPPED) != 0;
        boolean locked = (flags & LOCKED) != 0;
        boolean selected = (flags & SELECTED) != 0;
        boolean accent = equipped || selected;

        int bg = Theme.mix(Theme.SURFACE2, Theme.SURFACE3, hov);
        if (accent) bg = Theme.mix(bg, Theme.BRAND, 0.10f);
        g.rect(x, y, w, h, bg);
        // subtle brand glow around the active card
        if (accent) g.outline(x - 1, y - 1, w + 2, h + 2, Theme.alpha(Theme.BRAND, 0.25f));
        g.outline(x, y, w, h, accent ? Theme.BRAND : Theme.mix(Theme.LINE, Theme.LINE2, hov));

        int ts = w - 8;
        int tx = x + 4, ty = y + 4;
        g.rect(tx, ty, ts, ts, Theme.PANEL);
        thumbnail(g, item, tx, ty, ts);
        if (locked) g.rect(tx, ty, ts, ts, 0xA0000000);

        if (item.isDev()) Widgets.chip(g, "DEV", tx + 2, ty + 2, Theme.WARN);
        if (equipped) check(g, tx + ts - 12, ty + 2);
        else if (locked) lock(g, tx + ts - 11, ty + 2);
        if (badge != null) {
            int bw = g.width(badge) + 6;
            g.rect(tx + ts - bw - 2, ty + ts - 13, bw, 11, Theme.BRAND);
            g.text(badge, tx + ts - bw + 1, ty + ts - 11, 0xFFFFFFFF);
        }

        int textW = w - 8;
        int ny = ty + ts + 3;
        g.text(g.clip(item.name(), textW), tx, ny, locked ? Theme.DIM : Theme.mix(Theme.MUTED, Theme.TEXT, accent ? 1f : 0.6f + 0.4f * hov));
        if (locked && lockedReason != null) g.text(g.clip(lockedReason, textW), tx, ny + 10, Theme.DIM);
        else g.text(g.clip(rarityLabel(item.rarity()), textW), tx, ny + 10, locked ? Theme.alpha(RarityColors.of(item.rarity()), 0.5f) : RarityColors.of(item.rarity()));
    }

    /**
     * Thumbnail, or – for a DEV item pushed without one – a small live 3D preview of the item on the player, or a
     * monogram tile in the rarity colour while it loads / without one.
     */
    static void thumbnail(Gfx g, Item item, int x, int y, int size) {
        if (McThumbnails.draw(g.g, item, x, y, size)) return;
        if (item.isDev() && devPreview(g, item, x, y, size)) return;
        int m = Math.max(10, size / 2);
        Widgets.monogram(g, item.name(), x + (size - m) / 2, y + (size - m) / 2, m, RarityColors.of(item.rarity()));
    }

    /** Live previews of DEV items without a thumbnail (by id + revision), least recently used first out. */
    private static final int DEV_VIEWS_MAX = 12;
    private static final java.util.LinkedHashMap<String, AvatarPreview.View> DEV_VIEWS = new java.util.LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, AvatarPreview.View> e) {
            return size() > DEV_VIEWS_MAX;
        }
    };
    private static AvatarPreview devService;

    private static boolean devPreview(Gfx g, Item item, int x, int y, int size) {
        if (item.bundle() == null || size < 24) return false;
        AvatarPreview service = Services.get(AvatarPreview.class).orElse(null);
        if (service == null) return false;
        if (service != devService) {
            DEV_VIEWS.clear();
            devService = service;
        }
        String key = item.id() + "#" + item.revision();
        AvatarPreview.View v = DEV_VIEWS.get(key);
        try {
            if (v == null) {
                v = service.create();
                if (v == null) return false;
                for (Slot s : Slot.all()) v.override(s, null);
                if (item.isEmote()) v.playEmote(item.id());
                else v.override(item.slot(), item.id());
                Slot slot = item.slot();
                v.setYaw(slot == Slot.WINGS || slot == Slot.BACK || slot == Slot.CAPE ? 160f : 25f);
                v.setAutoRotate(false);
                DEV_VIEWS.put(key, v);
            }
            v.draw(g, x, y, size, size, 0f);
            return true;
        } catch (RuntimeException e) {
            DEV_VIEWS.remove(key);
            return false;
        }
    }

    static String rarityLabel(Rarity r) {
        return Lang.tr(RARITY_KEYS[r.ordinal()]);
    }

    private static final String[] RARITY_KEYS;

    static {
        Rarity[] all = Rarity.values();
        RARITY_KEYS = new String[all.length];
        for (int i = 0; i < all.length; i++) RARITY_KEYS[i] = "wardrobe.rarity." + all[i].id();
    }

    /** 10×10 brand square with a white check mark. */
    static void check(Gfx g, int x, int y) {
        g.rect(x, y, 10, 10, Theme.BRAND);
        int c = 0xFFFFFFFF;
        g.rect(x + 2, y + 5, 1, 1, c);
        g.rect(x + 3, y + 6, 1, 1, c);
        g.rect(x + 4, y + 7, 1, 1, c);
        g.rect(x + 5, y + 6, 1, 1, c);
        g.rect(x + 6, y + 5, 1, 1, c);
        g.rect(x + 7, y + 4, 1, 1, c);
        g.rect(x + 8, y + 3, 1, 1, c);
    }

    /** Small padlock (9×10). */
    static void lock(Gfx g, int x, int y) {
        int c = Theme.MUTED;
        g.rect(x + 2, y, 5, 1, c);
        g.rect(x + 1, y + 1, 1, 3, c);
        g.rect(x + 7, y + 1, 1, 3, c);
        g.rect(x, y + 4, 9, 6, c);
        g.rect(x + 4, y + 6, 1, 2, Theme.PANEL);
    }

    /** "+" drawn with rects (crisp at any size). */
    static void plus(Gfx g, int cx, int cy, int size, int color) {
        int t = Math.max(1, size / 6);
        g.rect(cx - size / 2, cy - t / 2, size, t, color);
        g.rect(cx - t / 2, cy - size / 2, t, size, color);
    }
}
