package dev.henny.vibeyclient.module.skins.ui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.launcher.LauncherStatus;
import dev.henny.vibeyclient.module.skins.SkinSwap;
import dev.henny.vibeyclient.module.skins.SkinsModule;
import dev.henny.vibeyclient.module.skins.core.LibrarySkin;
import dev.henny.vibeyclient.module.skins.core.SkinLibrary;
import dev.henny.vibeyclient.ui.CardScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.List;

public final class SkinLibraryScreen extends CardScreen {
    private static final int TILE_W = 96, TILE_H = 128, GAP = 8;
    private final SkinPreviews previews = new SkinPreviews();
    private long lastScan;

    public SkinLibraryScreen(Screen parent) {
        super(Component.literal(Lang.tr("skins.screen.title")), parent, "skins.screen.");
        SkinSwap s = swap();
        if (s != null) s.refresh();
        lastScan = System.currentTimeMillis();
    }

    public static void open(Screen parent) {
        ClientCompat.openScreen(new SkinLibraryScreen(parent));
    }

    private static SkinSwap swap() {
        SkinsModule m = SkinsModule.get();
        return m == null ? null : m.swap();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    protected int cardWidth() {
        return Math.min(560, width - 20);
    }

    @Override
    protected int fixedHeight() {
        return height - 30;
    }

    @Override
    protected int headerHeight() {
        return 46;
    }

    @Override
    protected int footerHeight() {
        return 40;
    }

    @Override
    protected void drawHeader(Gfx g, int x, int y, int w) {
        SkinSwap s = swap();
        if (s != null && System.currentTimeMillis() - lastScan > 2000) {
            lastScan = System.currentTimeMillis();
            s.refresh();
        }
        String title = tr("title");
        title(g, title, x + PAD, y + 13);
        SkinLibrary l = s == null ? null : s.library();
        if (l != null) g.text(tr("count", l.skins().size()), x + PAD + g.boldWidth(title) + 8, y + 13, DIM);
        boolean ready = s != null && s.availability() == SkinSwap.Availability.READY;
        String sub = l != null && l.accountName() != null ? (ready ? tr("account", l.accountName()) : tr("accountOnly", l.accountName()))
                : tr("subtitle");
        g.text(g.clip(sub, w - PAD * 2 - 30), x + PAD, y + 28, MUTED);
        closeButton(g, x, y, w);
    }

    @Override
    protected int drawBody(Gfx g, int x, int y, int w) {
        SkinSwap s = swap();
        if (s == null) return message(g, x, y, w, tr("off"));
        SkinSwap.Availability a = s.availability();
        switch (a) {
            case LOADING -> {
                g.text(tr("loading") + dots(), x, y + 8, MUTED);
                return y + 30;
            }
            case NO_LAUNCHER -> {
                return message(g, x, y, w, tr("noLauncher"));
            }
            case NO_LIST -> {
                return message(g, x, y, w, tr("noList"));
            }
            case WRONG_ACCOUNT -> {
                return message(g, x, y, w, tr("wrongAccount"));
            }
            default -> {
            }
        }
        SkinLibrary l = s.library();
        int yy = y;
        if (a == SkinSwap.Availability.CANNOT_CHANGE) yy = warning(g, x, yy, w, reason(l.reason())) + 8;
        if (l.skins().isEmpty()) return message(g, x, yy, w, tr("empty"));
        List<LibrarySkin> list = l.skins();
        int cols = Math.max(1, (w + GAP) / (TILE_W + GAP));
        int tw = (w - GAP * (cols - 1)) / cols;
        Path dir = LauncherStatus.instanceDir() == null ? null : LauncherStatus.vibeDir();
        boolean canClick = a == SkinSwap.Availability.READY && !s.busy();
        for (int i = 0; i < list.size(); i++) {
            int tx = x + (i % cols) * (tw + GAP);
            int ty = yy + (i / cols) * (TILE_H + GAP);
            tile(g, s, l, list.get(i), dir, tx, ty, tw, canClick);
        }
        int rows = (list.size() + cols - 1) / cols;
        return yy + rows * (TILE_H + GAP);
    }

    private void tile(Gfx g, SkinSwap s, SkinLibrary l, LibrarySkin skin, Path dir, int x, int y, int w, boolean canClick) {
        boolean active = l.isActive(skin);
        boolean pending = skin.id().equals(s.pendingSkin());
        boolean hov = canClick && !active && hover(x, y, w, TILE_H);
        g.rect(x, y, w, TILE_H, hov ? SURFACE_HOVER : SURFACE);
        int line = active ? Theme.alpha(GREEN, 0.9f) : pending || hov ? Theme.alpha(ACCENT, 0.85f) : Theme.LINE2;
        g.outline(x, y, w, TILE_H, line);
        boolean visible = y + TILE_H > 0 && y < height;
        Identifier tex = visible ? previews.get(dir, skin) : null;
        int scale = 3;
        int fx = x + (w - SkinFigure.W * scale) / 2, fy = y + 8;
        if (tex != null) SkinFigure.draw(g, tex, fx, fy, scale, skin.slim());
        else g.textCenter(dots(), x + w / 2, y + 50, DIM);
        g.text(g.clip(skin.name(), w - 10), x + 5, y + TILE_H - 24, TEXT);
        String tag = pending ? tr(s.phase() == SkinSwap.Phase.WORKING ? "working" : "sent") + dots()
                : active ? tr("active") : tr(skin.slim() ? "slim" : "classic");
        g.text(g.clip(tag, w - 10), x + 5, y + TILE_H - 12, active ? GREEN : pending ? ACCENT : DIM);
        if (canClick && !active) hit(x, y, w, TILE_H, () -> s.apply(skin));
    }

    private int warning(Gfx g, int x, int y, int w, String text) {
        List<String> lines = wrap(g, text, w - 20);
        int h = lines.size() * 11 + 12;
        g.rect(x, y, w, h, Theme.alpha(AMBER, 0.08f));
        g.outline(x, y, w, h, Theme.alpha(AMBER, 0.5f));
        int yy = y + 7;
        for (String line : lines) {
            g.text(line, x + 10, yy, AMBER);
            yy += 11;
        }
        return y + h;
    }

    private int message(Gfx g, int x, int y, int w, String text) {
        int yy = y + 10;
        for (String line : wrap(g, text, w)) {
            g.textCenter(line, x + w / 2, yy, MUTED);
            yy += 11;
        }
        return yy + 10;
    }

    private String reason(String code) {
        String c = code == null ? "" : code;
        return switch (c) {
            case "offline_account", "no_account", "signed_out" -> tr("cannot." + c);
            default -> tr("cannot.other");
        };
    }

    @Override
    protected void drawFooter(Gfx g, int x, int y, int w) {
        SkinSwap s = swap();
        int by = y + 11;
        String status = status(s);
        int right = x + w - PAD;
        int left = buttonRight(g, right, by, 18, tr("reload"), Style.SECONDARY, () -> {
            if (s != null) s.refresh();
        });
        if (status != null) {
            int color = s.phase() == SkinSwap.Phase.FAILED ? RED : s.phase() == SkinSwap.Phase.DONE ? GREEN : MUTED;
            g.text(g.clip(status, left - x - PAD * 2), x + PAD, by + 5, color);
        }
    }

    private String status(SkinSwap s) {
        if (s == null) return null;
        return switch (s.phase()) {
            case IDLE -> s.availability() == SkinSwap.Availability.READY ? tr("hint") : null;
            case SENT -> tr("sent") + dots();
            case WORKING -> tr("working") + dots();
            case DONE -> tr("done");
            case FAILED -> s.failure();
        };
    }

    @Override
    public void onClose() {
        previews.close();
        super.onClose();
    }
}
