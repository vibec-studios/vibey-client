package dev.henny.vibeyclient.module.cosmetics.ui;

import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.Cosmetico;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;

/**
 * The emote wheel as a square ring: a 3×3 grid of square tiles, the 8 outer tiles are the slots (0 at the top,
 * clockwise: top, top right, right, bottom right, bottom, bottom left, left, top left) and the centre tile shows
 * information. Shared by the wheel overlay (selection by mouse direction) and the wheel editor (selection by click).
 */
final class WheelLayout {
    static final int SLOTS = 8;
    private static final int[] COL = {1, 2, 2, 2, 1, 0, 0, 0};
    private static final int[] ROW = {0, 0, 1, 2, 2, 2, 1, 0};

    int x, y, tile, gap;

    /** Centres the grid on cx, cy. */
    void set(int cx, int cy, int tile, int gap) {
        this.tile = tile;
        this.gap = gap;
        int s = size();
        this.x = cx - s / 2;
        this.y = cy - s / 2;
    }

    int size() {
        return tile * 3 + gap * 2;
    }

    int tileX(int i) {
        return x + COL[i] * (tile + gap);
    }

    int tileY(int i) {
        return y + ROW[i] * (tile + gap);
    }

    int centreX() {
        return x + tile + gap;
    }

    int centreY() {
        return y + tile + gap;
    }

    /** Slot under the point, or -1. */
    int hit(double mx, double my) {
        for (int i = 0; i < SLOTS; i++) {
            int tx = tileX(i), ty = tileY(i);
            if (mx >= tx && mx < tx + tile && my >= ty && my < ty + tile) return i;
        }
        return -1;
    }

    /**
     * Slot in the direction of (dx, dy) from the centre (screen coordinates, y down), or -1 inside the dead zone.
     * Each slot owns a 45° sector centred on its direction.
     */
    static int direction(double dx, double dy, double deadZone) {
        if (dx * dx + dy * dy < deadZone * deadZone) return -1;
        double angle = Math.atan2(dx, -dy); // 0 = up, clockwise positive
        int i = (int) Math.round(angle / (Math.PI / 4));
        return Math.floorMod(i, SLOTS);
    }

    /**
     * One slot tile. {@code t} = 0..1 highlight animation, {@code a} = 0..1 overall alpha (open fade).
     * Empty slots show a "+".
     */
    static void drawTile(Gfx g, int x, int y, int s, String emoteId, float t, boolean selected, float a) {
        int grow = Math.round(t * 2);
        x -= grow;
        y -= grow;
        s += grow * 2;
        int bg = Theme.mix(Theme.SURFACE2, Theme.mix(Theme.SURFACE3, Theme.BRAND, 0.22f), Math.max(t, selected ? 1f : 0f));
        g.rect(x, y, s, s, Theme.alpha(bg, a * 0.96f));
        if (selected || t > 0.01f) {
            g.outline(x - 1, y - 1, s + 2, s + 2, Theme.alpha(Theme.BRAND, a * 0.3f * Math.max(t, selected ? 1f : 0f)));
            g.outline(x, y, s, s, Theme.alpha(Theme.mix(Theme.LINE2, Theme.BRAND, Math.max(t, selected ? 1f : 0f)), a));
        } else {
            g.outline(x, y, s, s, Theme.alpha(Theme.LINE2, a));
        }
        if (emoteId == null) {
            plus(g, x + s / 2, y + s / 2, Math.max(7, s / 5), Theme.alpha(Theme.mix(Theme.DIM, Theme.TEXT, t), a));
            return;
        }
        Item item = Cosmetico.items().get(emoteId);
        int nameH = s >= 40 ? 12 : 0;
        int ts = Math.min(s - 10, s - nameH - 8);
        int tx = x + (s - ts) / 2;
        int ty = y + 4 + (s - nameH - 8 - ts) / 2;
        if (item != null) {
            if (a > 0.6f) Cards.thumbnail(g, item, tx, ty, ts);
            if (nameH > 0) g.textCenter(g.clip(item.name(), s - 6), x + s / 2, y + s - 11, Theme.alpha(Theme.mix(Theme.MUTED, Theme.TEXT, t), a));
        } else {
            // assigned but not (yet) in the registry: show the id path
            String path = emoteId.substring(emoteId.indexOf(':') + 1);
            g.textCenter(g.clip(path, s - 6), x + s / 2, y + (s - 8) / 2, Theme.alpha(Theme.DIM, a));
        }
    }

    private static void plus(Gfx g, int cx, int cy, int size, int color) {
        Cards.plus(g, cx, cy, size, color);
    }
}
