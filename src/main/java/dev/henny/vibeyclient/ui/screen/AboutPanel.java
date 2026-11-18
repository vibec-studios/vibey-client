package dev.henny.vibeyclient.ui.screen;

import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.Panel;
import dev.henny.vibeyclient.ui.PixelFont;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Seal;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.VibeyTabs;
import dev.henny.vibeyclient.ui.Widgets;
import dev.henny.vibeyclient.ui.title.OnboardingScreen;

/**
 * "About" tab: the seal, version, Vibe Craft Studios, links, credits and the licenses of everything bundled
 * (incl. the lyrics fonts), plus "show the welcome again".
 */
public final class AboutPanel implements Panel {
    /** Our website (official builds; forks link their own, see SERVICES.md). */
    private static final String WEBSITE = "https://vibe.henny.dev";
    private static final String SOURCE = "https://github.com/vibec-studios/vibey-client";
    private static final String ISSUES = "https://github.com/vibec-studios/vibey-client/issues";

    /** name, license, holder – what ships inside the jar. */
    private static final String[][] LICENSES = {
            {"Vibey Client (code)", "Apache 2.0", "Vibe Craft Studios"},
            {"Artwork, seal, starter cosmetics", "All rights reserved", "Vibe Craft Studios"},
            {"Vibey Multiplayer / Vibey Lyrics modules", "MIT", "Vibe Craft Studios"},
            {"Poppins, Montserrat, Kanit, Fredoka, Unbounded", "SIL OFL 1.1", "their project authors"},
            {"Caveat, Bebas Neue, Bungee, Pacifico", "SIL OFL 1.1", "their project authors"},
            {"Press Start 2P, Monoton, Kaushan Script", "SIL OFL 1.1", "their project authors"},
            {"Permanent Marker", "Apache 2.0", "Font Diner"},
            {"JavaMP3 (lyrics audio)", "MIT", "delthas"},
    };

    private final ScrollArea scroll = new ScrollArea();
    private final Seal seal = new Seal(true);

    @Override
    public void draw(Ui ui, int x, int y, int w, int h) {
        Gfx g = ui.g;
        int pad = 16;
        int cy = scroll.begin(ui, x, y, w, h) + pad;
        int cx = x + pad;
        int cw = w - pad * 2;

        // hero: seal + wordmark + version
        float unit = cw >= 420 ? 2f : 1.5f;
        int sw = seal.width(unit), sh = seal.height(unit);
        boolean hov = ui.hover(cx, cy, sw, sh);
        if (ui.clickedNoFocus(cx, cy, sw, sh)) seal.cheer();
        seal.draw(g, cx, cy, unit, hov);
        int tx = cx + sw + 14;
        int ty = cy + Math.max(0, (sh - 52) / 2);
        PixelFont.drawExtruded(g, "VIBEY", tx, ty, 3, Theme.TEXT, Theme.BRAND);
        g.text("CLIENT", tx + PixelFont.width("VIBEY", 3) + 8, ty + 4, Theme.BRAND2);
        g.text(Lang.tr("core.about.by"), tx, ty + 22, Theme.TEXT);
        g.text(g.clip(VibeyScreen.versionLine(), cx + cw - tx), tx, ty + 34, Theme.DIM);
        cy += Math.max(sh, 52) + 14;

        // links
        int bx = cx;
        bx = link(ui, "web", bx, cy, Lang.tr("core.about.website"), WEBSITE);
        bx = link(ui, "src", bx, cy, Lang.tr("core.about.source"), SOURCE);
        bx = link(ui, "bug", bx, cy, Lang.tr("core.about.issues"), ISSUES);
        String welcome = Lang.tr("core.about.welcome");
        int ww = g.width(welcome) + 20;
        if (bx + ww > cx + cw) {
            cy += 22;
            bx = cx;
        }
        if (Widgets.button(ui, "welcome", bx, cy, ww, 16, welcome, Widgets.Style.PRIMARY)) {
            VibeyScreen menu = (VibeyScreen) ClientCompat.screen();
            ClientCompat.openScreen(new OnboardingScreen(new VibeyScreen(menu == null ? null : menu.parent(), VibeyTabs.ABOUT)));
        }
        cy += 30;

        // credits
        cy = section(g, Lang.tr("core.about.credits"), cx, cy, cw);
        cy = paragraph(g, Lang.tr("core.about.creditsText"), cx, cy, cw, Theme.MUTED) + 12;

        // privacy in one line (honest, same as onboarding)
        cy = section(g, Lang.tr("core.about.privacy"), cx, cy, cw);
        String online = GlobalSettings.online() ? Lang.tr("core.about.privacyOn", GlobalSettings.backend()) : Lang.tr("core.about.privacyOff");
        cy = paragraph(g, online, cx, cy, cw, Theme.MUTED) + 12;

        // licenses
        cy = section(g, Lang.tr("core.about.licenses"), cx, cy, cw);
        int licW = 0;
        for (String[] l : LICENSES) licW = Math.max(licW, g.width(l[1]));
        for (String[] l : LICENSES) {
            int rowH = 14;
            if (ui.visibleY(cy, rowH)) {
                g.text(g.clip(l[0], cw - licW - 16), cx, cy + 3, Theme.TEXT);
                g.textRight(l[1], cx + cw, cy + 3, Theme.BRAND2);
            }
            cy += rowH;
            g.hline(cx, cy - 1, cw, Theme.LINE);
        }
        cy += 8;
        cy = paragraph(g, Lang.tr("core.about.mojang"), cx, cy, cw, Theme.DIM);
        cy += pad;
        scroll.end(ui, cy);
    }

    private int link(Ui ui, Object key, int x, int y, String label, String url) {
        Gfx g = ui.g;
        int w = g.width(label) + Icons.SIZE + 20;
        boolean clicked = ui.clicked(x, y, w, 16);
        boolean hot = ui.focusedLast() || ui.hover(x, y, w, 16);
        float t = ui.anim(key, 0f).to(hot ? 1f : 0f);
        g.rect(x, y, w, 16, Theme.mix(Theme.SURFACE2, Theme.SURFACE3, t));
        g.outline(x, y, w, 16, Theme.mix(Theme.LINE2, 0x50FFFFFF, t));
        g.text(label, x + 7, y + 4, Theme.mix(Theme.MUTED, Theme.TEXT, t));
        Icons.draw(g, Icons.LINK, x + w - Icons.SIZE - 6, y + 3, Theme.mix(Theme.DIM, Theme.BRAND, t));
        ui.tooltip(key, x, y, w, 16, url);
        if (clicked) ClientCompat.openLink(ClientCompat.screen(), url);
        return x + w + 6;
    }

    private static int section(Gfx g, String title, int x, int y, int w) {
        String t = title.toUpperCase(java.util.Locale.ROOT);
        g.text(t, x, y, Theme.DIM);
        g.hline(x + g.width(t) + 6, y + 4, w - g.width(t) - 6, Theme.LINE);
        return y + 14;
    }

    private static int paragraph(Gfx g, String text, int x, int y, int w, int color) {
        for (String line : Ui.wrap(g, text, w)) {
            g.text(line, x, y, color);
            y += 10;
        }
        return y;
    }

    @Override
    public void onHide() {
        seal.close();
    }

    static String loader() {
        return Vibey.platform().loader() + " · Minecraft " + BuildInfo.MINECRAFT_LABEL;
    }
}
