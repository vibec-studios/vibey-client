package dev.henny.vibeyclient.ui.title;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Setting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.Anim;
import dev.henny.vibeyclient.ui.BaseScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.PixelFont;
import dev.henny.vibeyclient.ui.Seal;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * First-start welcome (shown once before the first Vibey title screen, reopenable from About): what Vibey is +
 * language, the online features explained honestly with an on/off choice, and where to find the menu and the emote
 * key. Esc / "Skip" also counts as done.
 */
public class OnboardingScreen extends BaseScreen {
    private static final int STEPS = 3;

    private final Screen next;
    private final Ui ui = new Ui(this::addWidget);
    private final Seal seal = new Seal(true);
    private final Anim open = new Anim(0f, 160);
    private final Anim stepAnim = new Anim(1f, 150);
    private final dev.henny.vibeyclient.ui.ScrollArea body = new dev.henny.vibeyclient.ui.ScrollArea();
    private int step;
    private int stepDir = 1;

    /** @param next the screen to show when done (the title screen, or the menu it was opened from) */
    public OnboardingScreen(Screen next) {
        super(Component.literal("Vibey Client"));
        this.next = next;
    }

    @Override
    protected void init() {
        ui.reattach();
    }

    @Override
    protected void draw(Gfx g, int mx, int my, float delta) {
        ui.begin(g, mx, my, delta);
        float o = open.to(1f);
        boolean world = Minecraft.getInstance().level != null;
        if (!world) panorama(g, delta);
        g.rect(0, 0, width, height, world ? Theme.SCRIM : 0xC8000000);

        int cw = Math.min(width - 24, 400);
        int ch = Math.min(height - 20, 236);
        int cx = (width - cw) / 2;
        int cy = (height - ch) / 2 + Math.round((1f - o) * 8);
        g.rect(cx, cy, cw, ch, Theme.SURFACE);
        g.outline(cx - 1, cy - 1, cw + 2, ch + 2, Theme.LINE2);

        // header: brand + step counter
        int pad = 14;
        PixelFont.drawExtruded(g, "VIBEY", cx + pad, cy + 11, 2, Theme.TEXT, Theme.BRAND);
        VibeyTitleScreen.spaced(g, "CLIENT", cx + pad + PixelFont.width("VIBEY", 2) + 7, cy + 12, Theme.BRAND2, 1);
        String counter = (step + 1) + " / " + STEPS;
        g.textRight(counter, cx + cw - pad, cy + 12, Theme.DIM);
        g.hline(cx, cy + 30, cw, Theme.LINE);

        // body, slides on step change
        float s = stepAnim.to(1f);
        int bodyY = cy + 31;
        int footY = cy + ch - 34;
        int off = Math.round((1f - s) * 8 * stepDir);
        // the body scrolls if a long translation does not fit a small window
        int by = body.begin(ui, cx, bodyY + 1, cw, footY - bodyY - 1) + 11;
        int end = switch (step) {
            case 0 -> welcome(g, cx + pad + off, by, cw - pad * 2, footY - bodyY - 12);
            case 1 -> online(g, cx + pad + off, by, cw - pad * 2);
            default -> controls(g, cx + pad + off, by, cw - pad * 2);
        };
        body.end(ui, end + 10);

        // footer
        g.hline(cx, footY, cw, Theme.LINE);
        int fy = footY + 9;
        int dx = cx + pad;
        for (int i = 0; i < STEPS; i++) {
            int dw = i == step ? 14 : 6;
            g.rect(dx, fy + 6, dw, 4, i == step ? Theme.BRAND : (i < step ? Theme.MUTED : Theme.SURFACE3));
            dx += dw + 4;
        }
        String nextLabel = Lang.tr(step == STEPS - 1 ? "core.onboarding.done" : "core.onboarding.next");
        int nw = Math.max(70, g.width(nextLabel) + 20);
        if (Widgets.button(ui, "next", cx + cw - pad - nw, fy, nw, 16, nextLabel, Widgets.Style.PRIMARY)) advance(1);
        if (step > 0) {
            String back = Lang.tr("core.onboarding.back");
            int bw = Math.max(54, g.width(back) + 16);
            if (Widgets.button(ui, "back", cx + cw - pad - nw - 6 - bw, fy, bw, 16, back, Widgets.Style.SECONDARY)) advance(-1);
        } else {
            String skip = Lang.tr("core.onboarding.skip");
            int sw = g.width(skip) + 16;
            if (Widgets.button(ui, "skip", cx + cw - pad - nw - 6 - sw, fy, sw, 16, skip, Widgets.Style.GHOST)) finish();
        }

        if (o < 1f) {
            g.layer();
            g.rect(cx - 1, cy - 1, cw + 2, ch + 2, Theme.alpha(Theme.SURFACE, 1f - o));
        }
        ui.end();
    }

    // -------------------------------------------------------------------------------------------- steps

    private int welcome(Gfx g, int x, int y, int w, int h) {
        float unit = Math.max(1f, Math.min(2f, (h - 6) / 26f));
        int sw = seal.width(unit);
        int textX = x;
        int textW = w;
        if (w >= 300) {
            boolean hov = ui.hover(x + w - sw, y, sw, seal.height(unit));
            if (ui.clickedNoFocus(x + w - sw, y, sw, seal.height(unit))) seal.cheer();
            seal.draw(g, x + w - sw, y + 4, unit, hov);
            textW = w - sw - 12;
        }
        g.bold(Lang.tr("core.onboarding.welcome.title"), textX, y, Theme.TEXT);
        int ty = paragraph(g, Lang.tr("core.onboarding.welcome.body"), textX, y + 14, textW, Theme.MUTED);
        ty += 10;
        Widgets.caption(g, Lang.tr("core.onboarding.language"), textX, ty);
        ty += 12;
        GlobalSettings.Language[] langs = GlobalSettings.Language.values();
        int bx = textX;
        int segW = 0;
        for (GlobalSettings.Language l : langs) segW = Math.max(segW, g.width(Lang.tr("core.setting.language." + l.name().toLowerCase(java.util.Locale.ROOT))) + 16);
        segW = Math.min(segW, (textW - 4) / langs.length);
        for (GlobalSettings.Language l : langs) {
            String label = Lang.tr("core.setting.language." + l.name().toLowerCase(java.util.Locale.ROOT));
            boolean sel = GlobalSettings.language.get() == l;
            if (Widgets.button(ui, l, bx, ty, segW, 16, label, sel ? Widgets.Style.PRIMARY : Widgets.Style.SECONDARY)) {
                GlobalSettings.language.set(l);
                Lang.tick();
            }
            bx += segW + 2;
        }
        return Math.max(ty + 16, y + seal.height(unit) + 4);
    }

    private int online(Gfx g, int x, int y, int w) {
        g.bold(Lang.tr("core.onboarding.online.title"), x, y, Theme.TEXT);
        int ty = paragraph(g, Lang.tr("core.onboarding.online.body", hostOf(GlobalSettings.backend())), x, y + 14, w, Theme.MUTED);
        ty += 4;
        ty = bullet(g, Icons.CHECK, Lang.tr("core.onboarding.online.token"), x, ty, w, Theme.OK);
        ty = bullet(g, Icons.INFO, Lang.tr("core.onboarding.online.data"), x, ty, w, Theme.MUTED);
        ty += 6;
        int half = (w - 6) / 2;
        boolean on = GlobalSettings.online();
        int ch = Math.max(choiceHeight(g, Lang.tr("core.onboarding.online.onDesc"), half), choiceHeight(g, Lang.tr("core.onboarding.online.offDesc"), half));
        if (choice(g, "on", x, ty, half, ch, Lang.tr("core.onboarding.online.on"), Lang.tr("core.onboarding.online.onDesc"), on)) GlobalSettings.onlineFeatures.set(true);
        if (choice(g, "off", x + half + 6, ty, half, ch, Lang.tr("core.onboarding.online.off"), Lang.tr("core.onboarding.online.offDesc"), !on)) GlobalSettings.onlineFeatures.set(false);
        return ty + ch;
    }

    private int controls(Gfx g, int x, int y, int w) {
        g.bold(Lang.tr("core.onboarding.keys.title"), x, y, Theme.TEXT);
        int ty = y + 16;
        String menuKey = ClientCompat.keyName(GlobalSettings.openMenu.mapping()).getString();
        KeybindSetting emote = emoteKey();
        String emoteName = emote == null ? null : ClientCompat.isUnbound(emote.mapping()) ? "—" : ClientCompat.keyName(emote.mapping()).getString();
        int col = Math.max(g.width(menuKey), Math.max(g.width("Esc"), emoteName == null ? 0 : g.width(emoteName))) + 10;
        ty = keyRow(g, menuKey, Lang.tr("core.onboarding.keys.menu"), x, ty, w, col);
        if (emoteName != null) ty = keyRow(g, emoteName, Lang.tr("core.onboarding.keys.emote"), x, ty, w, col);
        ty = keyRow(g, "Esc", Lang.tr("core.onboarding.keys.pause"), x, ty, w, col);
        ty += 4;
        return paragraph(g, Lang.tr("core.onboarding.keys.later"), x, ty, w, Theme.DIM);
    }

    // -------------------------------------------------------------------------------------------- bits

    private int paragraph(Gfx g, String text, int x, int y, int w, int color) {
        for (String line : Ui.wrap(g, text, w)) {
            g.text(line, x, y, color);
            y += 10;
        }
        return y;
    }

    private int bullet(Gfx g, String icon, String text, int x, int y, int w, int iconColor) {
        Icons.draw(g, icon, x, y, iconColor);
        int ny = paragraph(g, text, x + 14, y + 1, w - 14, Theme.TEXT);
        return ny + 3;
    }

    private int keyRow(Gfx g, String key, String text, int x, int y, int w, int col) {
        int kw = Math.max(22, g.width(key) + 10);
        g.rect(x, y, kw, 16, Theme.SURFACE3);
        g.outline(x, y, kw, 16, Theme.LINE2);
        g.hline(x + 1, y + 15, kw - 2, Theme.LINE2);
        g.textCenter(key, x + kw / 2, y + 4, Theme.TEXT);
        int tx = x + Math.max(col, 22) + 10;
        List<String> lines = Ui.wrap(g, text, w - (tx - x));
        int ly = y + (lines.size() == 1 ? 4 : 0);
        for (String l : lines) {
            g.text(l, tx, ly, Theme.MUTED);
            ly += 10;
        }
        return Math.max(y + 22, ly + 6);
    }

    private static int choiceHeight(Gfx g, String desc, int w) {
        return 20 + Ui.wrap(g, desc, w - 26).size() * 10;
    }

    private boolean choice(Gfx g, Object key, int x, int y, int w, int h, String title, String desc, boolean selected) {
        List<String> lines = Ui.wrap(g, desc, w - 26);
        boolean clicked = ui.clicked(x, y, w, h);
        boolean hot = ui.focusedLast() || ui.hover(x, y, w, h);
        float t = ui.anim(key, 0f).to(hot ? 1f : 0f);
        float s = ui.anim(key, 5, selected ? 1f : 0f).to(selected ? 1f : 0f);
        g.rect(x, y, w, h, Theme.mix(Theme.SURFACE2, Theme.SURFACE3, t));
        g.outline(x, y, w, h, Theme.mix(Theme.mix(Theme.LINE2, 0x50FFFFFF, t), Theme.BRAND, s));
        // radio square
        g.outline(x + 8, y + 7, 8, 8, Theme.mix(Theme.MUTED, Theme.BRAND, s));
        if (s > 0f) g.rect(x + 10, y + 9, 4, 4, Theme.alpha(Theme.BRAND, s));
        g.text(title, x + 22, y + 7, Theme.TEXT);
        int ly = y + 18;
        for (String l : lines) {
            g.text(l, x + 22, ly, Theme.DIM);
            ly += 10;
        }
        return clicked;
    }

    /** The emote wheel key of the emotes module, if it has one. */
    private static KeybindSetting emoteKey() {
        VibeyModule m = ModuleManager.get().get("emotes");
        if (m == null || !m.status().loadable()) return null;
        for (Setting<?, ?> s : m.settings().all()) if (s instanceof KeybindSetting k) return k;
        return null;
    }

    private static String hostOf(String url) {
        String s = url.replaceFirst("^[a-z]+://", "");
        int slash = s.indexOf('/');
        return slash < 0 ? s : s.substring(0, slash);
    }

    private void advance(int dir) {
        int n = step + dir;
        if (n >= STEPS) {
            finish();
            return;
        }
        if (n < 0) return;
        step = n;
        stepDir = dir;
        stepAnim.snap(0f);
        body.reset();
        ui.clearFocus();
    }

    private void finish() {
        GlobalSettings.setOnboarded(true);
        ClientCompat.openScreen(next);
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
        if (keyCode == Keys.LEFT && step > 0) {
            advance(-1);
            return true;
        }
        if (keyCode == Keys.RIGHT) {
            advance(1);
            return true;
        }
        return ui.key(keyCode, ClientCompat.shiftDown(), false);
    }

    @Override
    public void onClose() {
        finish();
    }

    @Override
    public void removed() {
        seal.close();
    }

    @Override
    public boolean isPauseScreen() {
        return next != null && next.isPauseScreen();
    }
}
