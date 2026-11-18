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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/**
 * The emote wheel overlay: 8 square slots around a centre tile, picked by the mouse direction from the centre (dead
 * zone in the middle = nothing). Hold the wheel key, point, release = play. A quick tap (released within
 * {@link #TAP_MS} without pointing anywhere) keeps the wheel open until a click instead. Left click plays (or opens
 * the wheel editor on an empty slot), right click / Escape cancels. Never pauses the game.
 */
final class EmoteWheelScreen extends BaseScreen {
    static final long TAP_MS = 250;

    private final Screen parent;
    private final BooleanSupplier keyHeld;
    private final ScreenUi ui = new ScreenUi();
    private final WheelLayout wheel = new WheelLayout();
    private final Anim open = new Anim(0f);
    private final Anim[] tiles = new Anim[WheelLayout.SLOTS];
    private final String[] ids = new String[WheelLayout.SLOTS];
    private final long openedAt = System.currentTimeMillis();
    private final TextCache titleLines = new TextCache();
    private int hovered = -1;
    private boolean sticky;
    private boolean done;

    EmoteWheelScreen(Screen parent, BooleanSupplier keyHeld) {
        super(Component.literal("Emote wheel"));
        this.parent = parent;
        this.keyHeld = keyHeld;
        for (int i = 0; i < tiles.length; i++) tiles[i] = new Anim(0f);
    }

    /** Slot the mouse points at (0..7) or -1. */
    int hovered() {
        return hovered;
    }

    @Override
    public void tick() {
        if (done || sticky) return;
        boolean held;
        try {
            held = keyHeld.getAsBoolean();
        } catch (RuntimeException e) {
            held = false;
        }
        if (held) return;
        if (System.currentTimeMillis() - openedAt < TAP_MS && hovered < 0) {
            sticky = true; // tapped: stay open, pick with a click
            return;
        }
        finish(true);
    }

    @Override
    protected void draw(Gfx g, int mx, int my, float delta) {
        ui.begin(g, mx, my, delta);
        Wardrobe wd = Services.get(Wardrobe.class).orElse(null);
        for (int i = 0; i < ids.length; i++) ids[i] = wd == null ? null : wd.wheel(i);
        float o = open.to(1f);

        int min = Math.min(width, height);
        int tile = Math.max(30, Math.min(62, (min - 80) / 3 - 4));
        int gap = Math.max(3, tile / 12);
        int cx = width / 2;
        int cy = height / 2 - 14;
        wheel.set(cx, cy, tile, gap);
        hovered = WheelLayout.direction(mx - (wheel.centreX() + tile / 2.0), my - (wheel.centreY() + tile / 2.0), tile * 0.45);

        g.rect(0, 0, width, height, Theme.alpha(0x8C000000, o));

        // open animation: scale 0.9 -> 1 around the centre, fade in
        float s = 0.9f + 0.1f * o;
        g.push();
        g.translate(cx, cy);
        g.scale(s, s);
        g.translate(-cx, -cy);

        int size = wheel.size();
        int fx = wheel.x - gap * 2, fy = wheel.y - gap * 2, fs = size + gap * 4;
        g.rect(fx, fy, fs, fs, Theme.alpha(Theme.PANEL, o * 0.85f));
        g.outline(fx, fy, fs, fs, Theme.alpha(Theme.LINE2, o));

        for (int i = 0; i < WheelLayout.SLOTS; i++) {
            float t = tiles[i].to(i == hovered ? 1f : 0f);
            WheelLayout.drawTile(g, wheel.tileX(i), wheel.tileY(i), tile, ids[i], t, false, o);
        }
        drawCentre(g, wd, wheel.centreX(), wheel.centreY(), tile, o);
        g.pop();

        // hints below the wheel: what a click/release does, how to fill empty slots, how to cancel
        int hy = fy + fs + 8;
        boolean anyEmpty = false;
        for (String id : ids) if (id == null) anyEmpty = true;
        if (wd == null) {
            g.textCenter(Lang.tr("wardrobe.wheel.unavailable"), cx, hy, Theme.alpha(Theme.WARN, o));
        } else {
            String action;
            if (hovered >= 0 && ids[hovered] != null) action = Lang.tr(sticky ? "wardrobe.wheel.click" : "wardrobe.wheel.release");
            else if (hovered >= 0) action = Lang.tr("wardrobe.wheel.emptyClick");
            else action = Lang.tr("wardrobe.wheel.pick");
            g.textCenter(action, cx, hy, Theme.alpha(hovered >= 0 ? Theme.TEXT : Theme.MUTED, o));
        }
        hy += 12;
        if (wd != null && anyEmpty) {
            g.textCenter(Lang.tr("wardrobe.wheel.hintAssign"), cx, hy, Theme.alpha(Theme.MUTED, o));
            hy += 12;
        }
        g.textCenter(Lang.tr("wardrobe.wheel.cancel"), cx, hy, Theme.alpha(Theme.DIM, o));
        ui.end();
    }

    private void drawCentre(Gfx g, Wardrobe wd, int x, int y, int s, float a) {
        g.rect(x, y, s, s, Theme.alpha(Theme.SURFACE, a));
        g.outline(x, y, s, s, Theme.alpha(Theme.LINE, a));
        int cx = x + s / 2;
        String title;
        int color = Theme.TEXT;
        if (hovered >= 0 && ids[hovered] != null) {
            AvatarItem item = AvatarItems.get(ids[hovered]);
            title = item != null ? item.name() : ids[hovered];
        } else if (hovered >= 0) {
            title = Lang.tr("wardrobe.wheel.empty");
            color = Theme.MUTED;
        } else {
            title = Lang.tr("wardrobe.wheel.title");
            color = Theme.DIM;
        }
        // small direction marker on the centre tile's edge
        if (hovered >= 0) {
            int mx = cx + DX[hovered] * (s / 2 - 3) - 1;
            int my = y + s / 2 + DY[hovered] * (s / 2 - 3) - 1;
            g.rect(mx, my, 3, 3, Theme.alpha(Theme.BRAND, a));
        }
        java.util.List<String> lines = titleLines.wrap(g, title, s - 8);
        int n = Math.min(2, lines.size());
        int ty = y + (s - n * 10 + 2) / 2;
        for (int i = 0; i < n; i++) {
            String line = i == 1 && lines.size() > 2 ? g.clip(lines.get(1) + " " + lines.get(2), s - 8) : lines.get(i);
            g.textCenter(line, cx, ty + i * 10, Theme.alpha(color, a));
        }
    }

    private static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};

    // ============================================================================================ input

    @Override
    protected boolean click(double x, double y, int button) {
        if (done) return true;
        if (button == 1) {
            finish(false);
            return true;
        }
        if (button != 0) return true;
        Wardrobe wd = Services.get(Wardrobe.class).orElse(null);
        if (hovered >= 0 && wd != null && wd.wheel(hovered) == null) {
            done = true;
            WardrobeUi.openWheelEditor(parent);
            return true;
        }
        finish(hovered >= 0);
        return true;
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        return true;
    }

    /** Closes the wheel; plays the hovered emote if {@code play}. */
    private void finish(boolean play) {
        if (done) return;
        done = true;
        Wardrobe wd = Services.get(Wardrobe.class).orElse(null);
        String id = play && hovered >= 0 && wd != null ? wd.wheel(hovered) : null;
        ClientCompat.openScreen(parent);
        if (id != null) wd.playEmote(id);
    }

    @Override
    public void onClose() {
        if (done) {
            ClientCompat.openScreen(parent);
            return;
        }
        finish(false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
