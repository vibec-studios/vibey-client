package dev.henny.vibeyclient.ui.screen;

import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Panel;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The "Modules" tab: left column = global settings entry + every visible module with its on/off switch,
 * right = the settings of the selected entry (auto-generated, or the module's own {@link Panel}).
 */
public final class ModulesPanel implements Panel {
    private static final int LIST_W = 168;
    private static final Object GLOBAL = new Object();
    private static final Object SEARCH = new Object();
    private static final int SEARCH_H = 28;

    /** Remembered across openings of the menu. */
    private static Object lastSelected = GLOBAL;

    /** Which entry the next opened menu shows: a module id, or null for the global settings. */
    public static void preselect(String moduleId) {
        VibeyModule m = moduleId == null ? null : ModuleManager.get().get(moduleId);
        lastSelected = m == null ? GLOBAL : m;
    }

    private final ScrollArea listScroll = new ScrollArea();
    private final ScrollArea detailScroll = new ScrollArea();
    private final Map<VibeyModule, Panel> customPanels = new HashMap<>();
    private Object selected = lastSelected;
    private String query = "";

    @Override
    public void draw(Ui ui, int x, int y, int w, int h) {
        Gfx g = ui.g;
        int listW = Math.min(LIST_W, w / 3);
        g.rect(x, y, listW, h, Theme.PANEL);
        g.vline(x + listW, y, h, Theme.LINE);
        drawList(ui, x, y, listW, h);
        drawDetail(ui, x + listW + 1, y, w - listW - 1, h);
    }

    // ============================================================================================ left column

    private void drawList(Ui ui, int x, int y, int w, int h) {
        Gfx g = ui.g;
        // search (stays on top)
        var box = Widgets.textField(ui, SEARCH, x + 8, y + 7, w - 16, query, 40, q -> {
            query = q;
            listScroll.reset();
        });
        if (query.isEmpty() && !box.isFocused()) g.text(Lang.tr("core.menu.search"), x + 13, y + 11, Theme.DIM);
        dev.henny.vibeyclient.ui.Icons.draw(g, dev.henny.vibeyclient.ui.Icons.SEARCH, x + w - 8 - 13, y + 11, box.isFocused() ? Theme.BRAND : Theme.DIM);
        g.hline(x, y + SEARCH_H, w, Theme.LINE);

        int cy = listScroll.begin(ui, x, y + SEARCH_H + 1, w, h - SEARCH_H - 1);
        cy += 6;
        String q = query.trim().toLowerCase(java.util.Locale.ROOT);
        boolean any = false;
        if (matches(q, Lang.tr("core.menu.global"), GlobalSettings.GROUP)) {
            cy = entry(ui, GLOBAL, null, x, cy, w);
            cy += 8;
            any = true;
        }
        boolean caption = false;
        List<VibeyModule> modules = ModuleManager.get().all();
        for (VibeyModule m : modules) {
            if (!m.visible() || !matches(q, m.displayName() + " " + Lang.tr(m.descriptionKey()), m.settings())) continue;
            if (!caption) {
                Widgets.caption(g, Lang.tr("core.menu.modules"), x + Theme.PAD, cy);
                cy += 13;
                caption = true;
            }
            cy = entry(ui, m, m, x, cy, w);
            any = true;
        }
        if (!any) {
            g.text(g.clip(Lang.tr("core.menu.noResults"), w - Theme.PAD * 2), x + Theme.PAD, cy + 4, Theme.DIM);
            cy += 20;
        }
        cy += 6;
        listScroll.end(ui, cy);
    }

    /** Search: name/description or any setting label contains the query. */
    private static boolean matches(String q, String text, dev.henny.vibeyclient.api.setting.SettingsGroup settings) {
        if (q.isEmpty() || text.toLowerCase(java.util.Locale.ROOT).contains(q)) return true;
        for (dev.henny.vibeyclient.api.setting.Setting<?, ?> s : settings.all()) {
            if (s.isVisible() && Lang.tr(s.nameKey()).toLowerCase(java.util.Locale.ROOT).contains(q)) return true;
        }
        return false;
    }

    /** Built-in icon for the bundled modules (a module's own {@code icon()} texture wins). */
    private static String iconFor(VibeyModule m) {
        return switch (m.id()) {
            case "social" -> dev.henny.vibeyclient.ui.Icons.FRIENDS;
            case "cosmetics" -> dev.henny.vibeyclient.ui.Icons.SHIRT;
            case "emotes" -> dev.henny.vibeyclient.ui.Icons.WAVE;
            case "multiplayer" -> dev.henny.vibeyclient.ui.Icons.HOST;
            case "lyrics" -> dev.henny.vibeyclient.ui.Icons.NOTE;
            case "devlink" -> dev.henny.vibeyclient.ui.Icons.CUBE;
            default -> null;
        };
    }

    private int entry(Ui ui, Object key, VibeyModule m, int x, int y, int w) {
        Gfx g = ui.g;
        int h = Theme.ROW;
        boolean sel = selected == key;
        boolean runnable = m == null || m.status().loadable();
        int tx = x + w - Theme.PAD - Widgets.TOGGLE_W;
        int ty = y + (h - Widgets.TOGGLE_H) / 2;
        // the switch first, so its click is not taken by the row
        if (m != null && runnable && m.canToggle()) {
            if (Widgets.toggle(ui, m, tx, ty, m.isEnabled(), true)) ModuleManager.get().toggle(m.id());
        }
        if (ui.clicked(x, y, w, h)) select(key);
        float hov = ui.anim(key, 0f).to(sel ? 1f : ui.hover(x, y, w, h) ? 0.6f : 0f);
        float s = ui.anim(key, 5, sel ? 1f : 0f).to(sel ? 1f : 0f);
        if (hov > 0f) g.rect(x, y, w, h, Theme.alpha(Theme.SURFACE3, hov));
        if (s > 0f) g.rect(x, y + Math.round(h * (1f - s) / 2f), 2, Math.max(1, Math.round(h * s)), Theme.BRAND);

        int ix = x + Theme.PAD;
        int iy = y + (h - 14) / 2;
        String name;
        if (m == null) {
            iconTile(g, dev.henny.vibeyclient.ui.Icons.GEAR, ix, iy, Theme.MUTED);
            name = Lang.tr("core.menu.global");
        } else {
            String builtin = iconFor(m);
            if (m.icon() != null) g.image(Compat.id(m.icon().substring(0, m.icon().indexOf(':')), m.icon().substring(m.icon().indexOf(':') + 1)), ix, iy, 14, 14, 16);
            else if (builtin != null) iconTile(g, builtin, ix, iy, runnable ? m.accentColor() : Theme.DIM);
            else Widgets.monogram(g, m.displayName(), ix, iy, 14, runnable ? m.accentColor() : Theme.DIM);
            name = m.displayName();
            ui.tooltip(m, x, y, Math.max(1, (m.canToggle() && runnable ? tx - 6 : x + w) - x), h, Lang.tr(m.descriptionKey()));
        }
        int textRight = m == null || !runnable ? x + w - Theme.PAD : tx - 6;
        int color = runnable ? (sel ? Theme.TEXT : Theme.mix(Theme.MUTED, Theme.TEXT, hov)) : Theme.DIM;
        g.text(g.clip(name, textRight - ix - 20), ix + 20, y + 8, color);
        if (m != null && !runnable) {
            String tag = m.status() == ModuleManager.Status.CONFLICT ? "MOD" : "—";
            g.textRight(tag, x + w - Theme.PAD, y + 8, Theme.WARN);
        }
        return y + h;
    }

    private static void iconTile(Gfx g, String icon, int x, int y, int color) {
        g.rect(x, y, 14, 14, Theme.alpha(color, 0.16f));
        g.outline(x, y, 14, 14, Theme.alpha(color, 0.55f));
        dev.henny.vibeyclient.ui.Icons.draw(g, icon, x + 3, y + 3, 1, color);
    }

    @Override
    public boolean key(int keyCode) {
        // Esc in a non-empty search clears it first
        if (keyCode == dev.henny.vibeyclient.client.Keys.ESCAPE && !query.isEmpty()) {
            query = "";
            listScroll.reset();
            return true;
        }
        return false;
    }

    private void select(Object key) {
        if (selected == key) return;
        Panel old = selected instanceof VibeyModule m ? customPanels.get(m) : null;
        if (old != null) old.onHide();
        selected = key;
        lastSelected = key;
        detailScroll.reset();
        if (key instanceof VibeyModule m) {
            Panel p = panelOf(m);
            if (p != null) p.onShow();
        }
    }

    private Panel panelOf(VibeyModule m) {
        if (!customPanels.containsKey(m)) customPanels.put(m, m.createSettingsPanel());
        return customPanels.get(m);
    }

    // ============================================================================================ right side

    private void drawDetail(Ui ui, int x, int y, int w, int h) {
        Gfx g = ui.g;
        if (selected instanceof VibeyModule m && !m.visible()) select(GLOBAL);
        int pad = Theme.PAD + 4;
        int cx = x + pad;
        int cw = w - pad * 2;
        if (selected instanceof VibeyModule m) {
            Panel custom = panelOf(m);
            if (custom != null && m.status().loadable()) {
                custom.draw(ui, x, y, w, h);
                return;
            }
            int cy = detailScroll.begin(ui, x, y, w, h) + pad;
            cy = header(ui, m, cx, cy, cw);
            cy = SettingsView.draw(ui, m.settings(), cx, cy, cw, m.status().loadable()) + pad;
            detailScroll.end(ui, cy);
        } else {
            int cy = detailScroll.begin(ui, x, y, w, h) + pad;
            g.bold(Lang.tr("core.menu.global"), cx, cy, Theme.TEXT);
            cy += 13;
            g.text(g.clip(Lang.tr("core.menu.globalDescription"), cw), cx, cy, Theme.MUTED);
            cy += 18;
            cy = SettingsView.draw(ui, GlobalSettings.GROUP, cx, cy, cw, true);
            cy += 12;
            g.text(Vibey.NAME + " " + dev.henny.vibeyclient.BuildInfo.VERSION + " · " + Vibey.platform().loader() + " · Minecraft "
                    + dev.henny.vibeyclient.BuildInfo.MINECRAFT_LABEL, cx, cy, Theme.DIM);
            cy += 12 + pad;
            detailScroll.end(ui, cy);
        }
    }

    private int header(Ui ui, VibeyModule m, int x, int y, int w) {
        Gfx g = ui.g;
        String name = m.displayName();
        g.bold(name, x, y, Theme.TEXT);
        int chipX = x + g.boldWidth(name) + 8;
        switch (m.status()) {
            case ENABLED -> Widgets.chip(g, Lang.tr("core.module.status.enabled"), chipX, y - 2, Theme.OK);
            case DISABLED -> Widgets.chip(g, Lang.tr("core.module.status.disabled"), chipX, y - 2, Theme.MUTED);
            default -> Widgets.chip(g, Lang.tr("core.module.status.off"), chipX, y - 2, Theme.WARN);
        }
        if (m.status().loadable() && m.canToggle()) {
            String label = Lang.tr(m.isEnabled() ? "core.module.disable" : "core.module.enable");
            int bw = Math.max(64, g.width(label) + 16);
            if (Widgets.button(ui, name, x + w - bw, y - 4, bw, 16, label,
                    m.isEnabled() ? Widgets.Style.SECONDARY : Widgets.Style.PRIMARY)) {
                ModuleManager.get().toggle(m.id());
            }
        }
        y += 14;
        g.text(g.clip(Lang.tr(m.descriptionKey()), w), x, y, Theme.MUTED);
        y += 14;
        String reason = m.statusReason();
        if (reason != null) {
            g.rect(x, y, w, 20, Theme.alpha(Theme.WARN, 0.10f));
            g.vline(x, y, 20, Theme.WARN);
            g.text(g.clip(reason, w - 12), x + 8, y + 6, Theme.WARN);
            y += 26;
        }
        return y + 6;
    }

    @Override
    public void onHide() {
        Panel p = selected instanceof VibeyModule m ? customPanels.get(m) : null;
        if (p != null) p.onHide();
    }
}
