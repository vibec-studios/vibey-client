package dev.henny.vibeyclient.ui.title;

import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.MenuPreview;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.Anim;
import dev.henny.vibeyclient.ui.BaseScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.MenuEntries;
import dev.henny.vibeyclient.ui.PixelFont;
import dev.henny.vibeyclient.ui.Seal;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The Vibey main menu that replaces the vanilla title screen ({@link TitleHook}): the vanilla panorama under a dark
 * layer, a left sidebar with the seal, the pixel wordmark and the navigation ({@link MenuEntries.Place#TITLE},
 * keyboard: arrows/Tab + Enter), the right side for the player preview ({@link MenuPreview}, the seal on its ice
 * floe while none is registered), and a status bar with version, backend status and Mojang's copyright line.
 */
public class VibeyTitleScreen extends BaseScreen {
    private static final int BAR = 16;

    private final Ui ui = new Ui(this::addWidget);
    private final Seal brandSeal = new Seal(false);
    private final Seal heroSeal = new Seal(true);
    private final Anim intro = new Anim(0f, 160);
    private final Anim fade = new Anim(1f, 700);
    private final boolean fadeFromBlack;
    private boolean previewShown;
    private boolean draggingPreview;
    // preview box of the last frame (for mouse forwarding)
    private int pvX, pvY, pvW, pvH;

    public VibeyTitleScreen(boolean fadeFromBlack) {
        super(Component.translatable("narrator.screen.title"));
        this.fadeFromBlack = fadeFromBlack;
    }

    @Override
    protected void init() {
        dev.henny.vibeyclient.ui.UiScale.apply(this);
        ui.reattach();
        BackendStatus.refresh();
        TitleHook.shownOnce = true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    // ============================================================================================ drawing

    @Override
    protected void draw(Gfx g, int mx, int my, float delta) {
        ui.begin(g, mx, my, delta);
        float o = intro.to(1f);
        float black = fadeFromBlack ? fade.to(0f) : 0f;

        panorama(g, delta);
        int W = width, H = height;
        int navW = Math.max(150, Math.min(210, Math.round(W * 0.30f)));
        int slide = Math.round((1f - o) * -6);

        // layers: dim the panorama, darker sidebar, status bar
        g.rect(0, 0, W, H, 0x8C000000);
        g.rect(0, 0, navW, H - BAR, 0xD908080B);
        g.vline(navW, 0, H - BAR, Theme.LINE2);

        drawSidebar(g, slide, 0, navW, H - BAR);
        drawPreview(g, navW + 1, 0, W - navW - 1, H - BAR, mx, my, delta);
        drawTopRight(g, W, mx, my);
        drawStatusBar(g, W, H);

        // fade the content in (overlay, so text fades too)
        if (o < 1f || black > 0f) {
            g.layer();
            if (black > 0f) g.rect(0, 0, W, H, Theme.alpha(0xFF000000, black));
            if (o < 1f) g.rect(0, 0, navW, H - BAR, Theme.alpha(0xFF08080B, (1f - o) * 0.85f));
        }
        ui.end();
    }

    // -------------------------------------------------------------------------------------------- sidebar

    private void drawSidebar(Gfx g, int slide, int y0, int w, int h) {
        int pad = 12;
        List<MenuEntries.Entry> entries = MenuEntries.visible(MenuEntries.Place.TITLE);
        MenuEntries.Entry quit = null;
        for (MenuEntries.Entry e : entries) if (e.order() >= 1000) quit = e;
        int itemCount = entries.size() - (quit == null ? 0 : 1);
        int seps = groups(entries);

        // brand block size: shrink when the screen is short
        boolean big = h >= 300 && w >= 170;
        int cell = big ? 3 : 2;
        float unit = big ? 1.5f : 1.0f;
        int itemH = h >= 270 ? 20 : 18;
        int quitY = h - pad - itemH + 2;
        int brandH = Math.max(brandSeal.height(unit), PixelFont.height(cell) + 13);
        int navTop = y0 + pad + brandH + (h >= 260 ? 18 : 10);
        int need = itemCount * itemH + seps * 9;
        if (navTop + need > quitY - 6) {
            itemH = 16;
            need = itemCount * itemH + seps * 7;
        }
        boolean showSeal = navTop + need <= quitY - 6 || h >= 200;

        // brand: seal + VIBEY + CLIENT
        int bx = pad + slide;
        int by = y0 + pad;
        int sealW = 0;
        if (showSeal) {
            int sh = brandSeal.height(unit);
            boolean hov = ui.hover(bx, by, brandSeal.width(unit), sh);
            if (ui.clickedNoFocus(bx, by, brandSeal.width(unit), sh)) brandSeal.cheer();
            sealW = brandSeal.draw(g, bx, by + Math.max(0, (brandH - sh) / 2), unit, hov) + 6;
        }
        int wx = bx + sealW;
        int wordH = PixelFont.height(cell);
        int wy = by + Math.max(0, (brandH - wordH - 13) / 2);
        PixelFont.drawExtruded(g, "VIBEY", wx, wy, cell, Theme.TEXT, Theme.BRAND);
        spaced(g, "CLIENT", wx, wy + wordH + cell + 4, Theme.BRAND2, 1);

        // navigation
        int y = navTop;
        int prevGroup = -1;
        for (MenuEntries.Entry e : entries) {
            if (e == quit) continue;
            int grp = group(e.order());
            if (prevGroup >= 0 && grp != prevGroup) {
                g.hline(pad + slide, y + (itemH >= 18 ? 4 : 3), w - pad * 2, Theme.LINE);
                y += itemH >= 18 ? 9 : 7;
            }
            prevGroup = grp;
            navItem(g, e, slide, y, w, itemH);
            y += itemH;
        }
        if (quit != null) {
            g.hline(pad + slide, quitY - 6, w - pad * 2, Theme.LINE);
            navItem(g, quit, slide, quitY, w, itemH);
        }
    }

    private static int group(int order) {
        if (order < 20) return 0;     // play
        if (order < 50) return 1;     // Vibey features
        if (order < 1000) return 2;   // options, mods
        return 3;                     // quit
    }

    private static int groups(List<MenuEntries.Entry> entries) {
        int seps = 0, prev = -1;
        for (MenuEntries.Entry e : entries) {
            int g = group(e.order());
            if (g == 3) continue;
            if (prev >= 0 && g != prev) seps++;
            prev = g;
        }
        return seps;
    }

    private void navItem(Gfx g, MenuEntries.Entry e, int slide, int y, int w, int h) {
        int x = 8 + slide;
        int iw = w - 16;
        boolean clicked = ui.clicked(x, y, iw, h);
        boolean focused = ui.focusedLast();
        boolean hot = focused || ui.hover(x, y, iw, h);
        float t = ui.anim(e, 0f).to(hot ? 1f : 0f);
        if (t > 0f) {
            g.rect(x, y, iw, h, Theme.alpha(Theme.SURFACE3, t));
            int bh = Math.max(1, Math.round((h - 6) * t));
            g.rect(x, y + (h - bh) / 2, 2, bh, Theme.BRAND);
        }
        boolean quit = e.order() >= 1000;
        int fg = Theme.mix(e.order() == 0 ? Theme.TEXT : Theme.MUTED, quit ? Theme.DANGER : Theme.TEXT, t);
        int iconColor = Theme.mix(Theme.DIM, quit ? Theme.DANGER : Theme.BRAND, t);
        if (Icons.has(e.icon())) Icons.draw(g, e.icon(), x + 10, y + (h - Icons.SIZE) / 2, iconColor);
        String label = Lang.tr(e.labelKey());
        int badge = MenuEntries.badge(e);
        int right = x + iw - 8;
        if (badge > 0) {
            String b = badge > 99 ? "99+" : String.valueOf(badge);
            int bw = g.width(b) + 6;
            g.rect(right - bw, y + (h - 11) / 2, bw, 11, Theme.BRAND);
            g.text(b, right - bw + 3, y + (h - 11) / 2 + 2, 0xFFFFFFFF);
            right -= bw + 4;
        }
        g.text(g.clip(label, right - (x + 26)), x + 26 + Math.round(t * 2), y + (h - 8) / 2, fg);
        if (clicked) {
            ui.clearFocus();
            try {
                e.action().accept(this);
            } catch (RuntimeException ex) {
                Vibey.LOG.error("Title entry {} failed", e.id(), ex);
            }
        }
    }

    // -------------------------------------------------------------------------------------------- preview / hero

    private void drawPreview(Gfx g, int x, int y, int w, int h, int mx, int my, float delta) {
        MenuPreview preview = Services.get(MenuPreview.class).orElse(null);
        int top = y + 30;
        int bottom = y + h - 30;
        String motd = BackendStatus.motd();
        if (motd != null && !motd.isEmpty()) drawMotd(g, x + 14, y + 12, Math.min(220, w - 28 - 70), motd);
        pvX = x + 12;
        pvY = top;
        pvW = w - 24;
        pvH = bottom - top;
        if (preview != null) {
            if (!previewShown) {
                previewShown = true;
                preview.onShow("title");
            }
            try {
                preview.render(g, pvX, pvY, pvW, pvH, mx, my, delta);
            } catch (RuntimeException e) {
                Vibey.LOG.debug("Menu preview failed: {}", e.toString());
            }
        } else {
            // the seal on its ice floe, with a soft brand glow
            float unit = Math.max(1f, Math.min(pvW * 0.55f / 36.5f, pvH * 0.62f / 26f));
            unit = Math.min(unit, 7f);
            int sw = heroSeal.width(unit), sh = heroSeal.height(unit);
            int sx = x + (w - sw) / 2;
            int sy = pvY + (pvH - sh) / 2 + 4;
            glow(g, x + w / 2, sy + sh * 2 / 3, sw, sh / 2);
            boolean hov = ui.hover(sx, sy, sw, sh);
            if (ui.clickedNoFocus(sx, sy, sw, sh)) heroSeal.cheer();
            heroSeal.draw(g, sx, sy, unit, hov);
        }
        // nameplate: square status dot + name
        String name = ClientCompat.playerName();
        int nw = g.width(name) + 22;
        int nx = x + (w - nw) / 2;
        int ny = bottom + 2;
        g.rect(nx, ny, nw, 16, 0xCC0B0B0E);
        g.outline(nx, ny, nw, 16, Theme.LINE2);
        BackendStatus.State st = BackendStatus.state();
        g.rect(nx + 7, ny + 6, 4, 4, st == BackendStatus.State.ONLINE ? Theme.OK : Theme.DIM);
        g.text(name, nx + 16, ny + 4, Theme.TEXT);
    }

    /** Soft purple glow under the floe: stacked translucent pixel ellipses (the only "gradient" the design allows). */
    private static void glow(Gfx g, int cx, int cy, int w, int h) {
        for (int i = 0; i < 4; i++) {
            float rx = w * (0.62f - i * 0.11f), ry = h * (0.55f - i * 0.1f);
            if (rx < 2 || ry < 2) break;
            int alpha = 0x0E + i * 4;
            int col = alpha << 24 | (Theme.BRAND & 0xFFFFFF);
            int step = Math.max(1, Math.round(ry / 10f));
            for (int dy = -Math.round(ry); dy < Math.round(ry); dy += step) {
                float f = (dy + step / 2f) / ry;
                int hw = Math.round(rx * (float) Math.sqrt(Math.max(0, 1 - f * f)));
                if (hw > 0) g.rect(cx - hw, cy + dy, hw * 2, step, col);
            }
        }
    }

    private void drawMotd(Gfx g, int x, int y, int maxW, String motd) {
        if (maxW < 80) return;
        List<String> lines = Ui.wrap(g, motd, maxW - 14);
        if (lines.size() > 3) lines = lines.subList(0, 3);
        int w = 0;
        for (String l : lines) w = Math.max(w, g.width(l));
        w = Math.max(w, g.width(Lang.tr("core.title.news"))) + 14;
        int h = 18 + lines.size() * 10;
        g.rect(x, y, w, h, 0xCC0B0B0E);
        g.outline(x, y, w, h, Theme.LINE2);
        g.rect(x, y, 2, h, Theme.BRAND2);
        spaced(g, Lang.tr("core.title.news").toUpperCase(java.util.Locale.ROOT), x + 7, y + 5, Theme.BRAND2, 0);
        int ty = y + 16;
        for (String l : lines) {
            g.text(l, x + 7, ty, Theme.TEXT);
            ty += 10;
        }
    }

    // -------------------------------------------------------------------------------------------- top right

    private void drawTopRight(Gfx g, int W, int mx, int my) {
        int s = 18, gap = 4;
        int x = W - 8 - s;
        int y = 8;
        if (iconButton(g, Icons.ACCESS, x, y, s, Lang.tr("core.title.accessibility"))) {
            ClientCompat.openScreen(new AccessibilityOptionsScreen(this, Minecraft.getInstance().options));
        }
        x -= s + gap;
        if (iconButton(g, Icons.GLOBE, x, y, s, Lang.tr("core.title.language"))) {
            Minecraft mc = Minecraft.getInstance();
            ClientCompat.openScreen(new LanguageSelectScreen(this, mc.options, mc.getLanguageManager()));
        }
        x -= s + gap;
        if (iconButton(g, Icons.MODULES, x, y, s, Lang.tr("core.title.classic"))) TitleHook.openVanilla();
    }

    private boolean iconButton(Gfx g, String icon, int x, int y, int s, String tip) {
        boolean clicked = ui.clicked(x, y, s, s);
        boolean hot = ui.focusedLast() || ui.hover(x, y, s, s);
        float t = ui.anim(icon, 0f).to(hot ? 1f : 0f);
        g.rect(x, y, s, s, Theme.mix(0xB30B0B0E, 0xF018181F, t));
        g.outline(x, y, s, s, Theme.mix(Theme.LINE2, 0x50FFFFFF, t));
        Icons.draw(g, icon, x + (s - Icons.SIZE) / 2, y + (s - Icons.SIZE) / 2, Theme.mix(Theme.MUTED, Theme.TEXT, t));
        ui.tooltip(icon, x, y, s, s, tip);
        return clicked;
    }

    // -------------------------------------------------------------------------------------------- status bar

    private void drawStatusBar(Gfx g, int W, int H) {
        int y = H - BAR;
        g.rect(0, y, W, BAR, 0xEB000000);
        g.hline(0, y, W, Theme.LINE2);
        int ty = y + 4;

        // right: Mojang's copyright line (required), opens the credits
        String copy = Component.translatable("title.credits").getString();
        int cw = g.width(copy);
        int cx = W - 8 - cw;
        boolean click = ui.clicked(cx - 2, y + 1, cw + 4, BAR - 1);
        boolean hot = ui.focusedLast() || ui.hover(cx - 2, y + 1, cw + 4, BAR - 1);
        g.text(copy, cx, ty, hot ? Theme.TEXT : Theme.MUTED);
        if (hot) g.hline(cx, ty + 9, cw, Theme.MUTED);
        if (click) ClientCompat.openScreen(new CreditsAndAttributionScreen(this));

        // left: version
        String full = "Vibey Client " + BuildInfo.VERSION + "  ·  Minecraft " + BuildInfo.MINECRAFT_LABEL + " " + Vibey.platform().loader();
        String mid = "Vibey " + BuildInfo.VERSION + " · " + BuildInfo.MINECRAFT_LABEL;
        String status = BackendStatus.label();
        BackendStatus.State st = BackendStatus.state();
        int avail = cx - 16 - 8;
        int statusW = g.width(status) + 10;
        String left = full;
        if (g.width(left) + 16 + statusW > avail) left = mid;
        boolean statusText = g.width(left) + 16 + statusW <= avail;
        if (!statusText && g.width(left) + 16 + 6 > avail) left = "Vibey " + BuildInfo.VERSION;
        g.text(left, 8, ty, Theme.DIM);
        int sx = 8 + g.width(left) + 12;
        g.vline(sx - 6, y + 4, 8, Theme.LINE2);
        int dot = BackendStatus.color(st);
        g.rect(sx, ty + 1, 5, 5, dot);
        if (st == BackendStatus.State.ONLINE) g.outline(sx - 1, ty, 7, 7, Theme.alpha(dot, 0.35f));
        if (statusText) g.text(status, sx + 9, ty, Theme.MUTED);
        ui.tooltip(st, sx - 2, y, statusText ? statusW + 4 : 10, BAR, Lang.tr("core.status.tip." + st.name().toLowerCase(java.util.Locale.ROOT)));
    }

    /** Text with extra spacing between letters (the launcher's mono labels). */
    static void spaced(Gfx g, String s, int x, int y, int color, int extra) {
        for (int i = 0; i < s.length(); i++) {
            String c = String.valueOf(s.charAt(i));
            g.text(c, x, y, color);
            x += g.width(c) + extra;
        }
    }

    // ============================================================================================ input

    @Override
    protected boolean click(double x, double y, int button) {
        MenuPreview p = Services.get(MenuPreview.class).orElse(null);
        if (p != null && inPreview(x, y) && p.mouseClicked(x, y, button)) {
            draggingPreview = true;
            return true;
        }
        return ui.queueClick(x, y, button);
    }

    @Override
    protected boolean drag(double x, double y, int button, double dx, double dy) {
        MenuPreview p = Services.get(MenuPreview.class).orElse(null);
        return draggingPreview && p != null && p.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    protected boolean release(double x, double y, int button) {
        ui.release();
        if (draggingPreview) {
            draggingPreview = false;
            MenuPreview p = Services.get(MenuPreview.class).orElse(null);
            return p != null && p.mouseReleased(x, y, button);
        }
        return false;
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        MenuPreview p = Services.get(MenuPreview.class).orElse(null);
        if (p != null && inPreview(x, y) && p.mouseScrolled(x, y, amount)) return true;
        ui.queueScroll(amount);
        return true;
    }

    private boolean inPreview(double x, double y) {
        return x >= pvX && x < pvX + pvW && y >= pvY && y < pvY + pvH;
    }

    @Override
    protected boolean key(int keyCode) {
        if (ui.anyInputFocused()) return false;
        return ui.key(keyCode, ClientCompat.shiftDown(), true);
    }

    @Override
    public void removed() {
        brandSeal.close();
        dev.henny.vibeyclient.ui.UiScale.restore();
        heroSeal.close();
        if (previewShown) {
            previewShown = false;
            Services.get(MenuPreview.class).ifPresent(p -> p.onHide("title"));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
