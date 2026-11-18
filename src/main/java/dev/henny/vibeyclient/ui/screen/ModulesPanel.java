package dev.henny.vibeyclient.ui.screen;

import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.Panel;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.VibeyTabs;
import dev.henny.vibeyclient.ui.Widgets;
import net.minecraft.client.gui.components.EditBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The "Modules" tab, like the mod screens of the big clients: a search bar and a grid of tiles (icon, name, one line
 * of description, on/off switch). A click on a tile opens the module's page (toolbar with back + switch, a short
 * intro, its buttons, then its settings via {@link SettingsView} or the module's own {@link Panel}). Creator modules
 * (category {@link ModuleCategory#CREATOR}) sit in a collapsed "For creators" area under the grid.
 */
public final class ModulesPanel implements Panel {
    private static final Object SEARCH = new Object(), BACK = new Object(), PAGE_TOGGLE = new Object(), CREATORS = new Object(),
            GENERAL_TILE = new Object();
    private static final int BAR_H = 30;
    private static final int TILE_H = 54;
    private static final int TILE_MIN_W = 150;
    private static final int GAP = 8;

    /** Module page the next opened menu shows (null = the grid); remembered across openings of the menu. */
    private static VibeyModule lastPage;
    private static boolean creatorsOpen;

    /** Last drawn bounds (x, y, w, h in GUI pixels) of each module tile, for tests that click them. */
    private static final Map<String, int[]> TILE_BOUNDS = new HashMap<>();

    /** Bounds of the tile of module {@code id} in the last frame, or null (tests). */
    public static int[] tileBounds(String id) {
        return TILE_BOUNDS.get(id);
    }

    /** Id of the module page that is open (or opens next), null = the grid (tests). */
    public static String openPage() {
        return lastPage == null ? null : lastPage.id();
    }

    /** Which module page the next opened menu shows: a module id, or null for the grid. */
    public static void preselect(String moduleId) {
        lastPage = moduleId == null ? null : ModuleManager.get().get(moduleId);
    }

    private final ScrollArea gridScroll = new ScrollArea();
    private final ScrollArea pageScroll = new ScrollArea();
    private final Map<VibeyModule, Panel> customPanels = new HashMap<>();
    private VibeyModule page = lastPage;
    private String query = "";
    private EditBox searchBox;
    private Ui ui;

    @Override
    public void draw(Ui ui, int x, int y, int w, int h) {
        this.ui = ui;
        if (page != null && !page.visible()) open(null);
        if (page != null) drawPage(ui, page, x, y, w, h);
        else drawGrid(ui, x, y, w, h);
    }

    // ============================================================================================ grid

    private void drawGrid(Ui ui, int x, int y, int w, int h) {
        Gfx g = ui.g;
        int pad = Theme.PAD + 4;
        // search bar (stays on top)
        int sx = x + pad, sw = w - pad * 2;
        EditBox box = Widgets.textField(ui, SEARCH, sx, y + 8, sw, query, 40, q -> {
            query = q;
            gridScroll.reset();
        });
        searchBox = box;
        if (query.isEmpty() && !box.isFocused()) g.text(g.clip(Lang.tr("core.menu.search"), sw - 30), sx + 6, y + 12, Theme.DIM);
        Icons.draw(g, Icons.SEARCH, sx + sw - 14, y + 12, box.isFocused() ? Theme.BRAND : Theme.DIM);

        int top = y + BAR_H;
        int cy = gridScroll.begin(ui, x, top, w, h - BAR_H) + 4;
        String q = query.trim().toLowerCase(Locale.ROOT);
        int cols = Math.max(1, (sw + GAP) / (TILE_MIN_W + GAP));
        int tileW = (sw - GAP * (cols - 1)) / cols;

        List<VibeyModule> main = new ArrayList<>();
        List<VibeyModule> creators = new ArrayList<>();
        Map<VibeyModule, String> hits = new HashMap<>();
        for (VibeyModule m : ModuleManager.get().all()) {
            if (!m.visible()) continue;
            if (!q.isEmpty()) {
                String text = (m.displayName() + " " + Lang.tr(m.descriptionKey())).toLowerCase(Locale.ROOT);
                String setting = text.contains(q) ? null : SettingsView.matchingSetting(m.settings(), q);
                if (!text.contains(q) && setting == null) continue;
                if (setting != null) hits.put(m, setting);
                main.add(m);
            } else if (m.category() == ModuleCategory.CREATOR) {
                creators.add(m);
            } else {
                main.add(m);
            }
        }
        String generalHit = q.isEmpty() ? null : SettingsView.matchingSetting(GlobalSettings.GROUP, q);
        if (generalHit == null && !q.isEmpty() && Lang.tr("core.tab.general").toLowerCase(Locale.ROOT).contains(q)) generalHit = "";

        int i = 0;
        for (VibeyModule m : main) {
            int tx = sx + (i % cols) * (tileW + GAP);
            int ty = cy + (i / cols) * (TILE_H + GAP);
            tile(ui, m, tx, ty, tileW, hits.get(m));
            i++;
        }
        if (generalHit != null) {
            int tx = sx + (i % cols) * (tileW + GAP);
            int ty = cy + (i / cols) * (TILE_H + GAP);
            generalTile(ui, tx, ty, tileW, generalHit);
            i++;
        }
        cy += ((i + cols - 1) / cols) * (TILE_H + GAP);
        if (i == 0) {
            Widgets.card(g, sx, cy, sw, 34);
            g.textCenter(Lang.tr("core.menu.noResults"), sx + sw / 2, cy + 13, Theme.DIM);
            cy += 34 + GAP;
        }

        if (!creators.isEmpty()) {
            cy += 4;
            String hint = Lang.tr("core.menu.creatorsHint");
            if (Widgets.disclosure(ui, CREATORS, sx, cy, sw, 22, Lang.tr("core.menu.creators"), hint, creatorsOpen)) {
                creatorsOpen = !creatorsOpen;
            }
            cy += 22 + GAP;
            if (creatorsOpen) {
                int j = 0;
                for (VibeyModule m : creators) {
                    tile(ui, m, sx + (j % cols) * (tileW + GAP), cy + (j / cols) * (TILE_H + GAP), tileW, null);
                    j++;
                }
                cy += ((j + cols - 1) / cols) * (TILE_H + GAP);
            }
        }
        gridScroll.end(ui, cy + 6);
    }

    /** Built-in icon for the bundled modules (a module's own {@code icon()} texture wins). */
    public static String iconFor(VibeyModule m) {
        return switch (m.id()) {
            case "social" -> Icons.FRIENDS;
            case "cosmetics" -> Icons.SHIRT;
            case "emotes" -> Icons.WAVE;
            case "multiplayer" -> Icons.HOST;
            case "lyrics", "media" -> Icons.NOTE;
            case "devlink" -> Icons.CUBE;
            case "nametags" -> Icons.SEAL;
            default -> null;
        };
    }

    /** The module's icon in a square tile of {@code size} px. */
    public static void moduleIcon(Gfx g, VibeyModule m, int x, int y, int size, boolean active) {
        int color = active ? m.accentColor() : Theme.DIM;
        String builtin = iconFor(m);
        if (m.icon() != null) {
            int c = m.icon().indexOf(':');
            g.image(Compat.id(m.icon().substring(0, c), m.icon().substring(c + 1)), x, y, size, size, 16);
        } else if (builtin != null) {
            Widgets.iconTile(g, builtin, x, y, size, Math.max(1, (size - 6) / Icons.SIZE), color);
        } else {
            Widgets.monogram(g, m.displayName(), x, y, size, color);
        }
    }

    private void tile(Ui ui, VibeyModule m, int x, int y, int w, String settingHit) {
        Gfx g = ui.g;
        if (!ui.visibleY(y, TILE_H)) return;
        int[] b = TILE_BOUNDS.computeIfAbsent(m.id(), k -> new int[4]);
        b[0] = x;
        b[1] = y;
        b[2] = w;
        b[3] = TILE_H;
        boolean runnable = m.status().loadable();
        boolean on = m.isEnabled();
        int tgx = x + w - 9 - Widgets.TOGGLE_W, tgy = y + 9;
        boolean toggleable = runnable && m.canToggle();
        // the switch first, so its click is not taken by the tile
        boolean toggled = toggleable && ui.clicked(tgx - 3, tgy - 3, Widgets.TOGGLE_W + 6, Widgets.TOGGLE_H + 6);
        boolean toggleHot = toggleable && (ui.focusedLast() || ui.hover(tgx - 3, tgy - 3, Widgets.TOGGLE_W + 6, Widgets.TOGGLE_H + 6));
        boolean clicked = ui.clicked(x, y, w, TILE_H);
        boolean hot = ui.focusedLast() || ui.hover(x, y, w, TILE_H);
        float hov = ui.anim(m, 0f).to(hot ? 1f : 0f);
        g.rect(x, y, w, TILE_H, Theme.mix(Theme.SURFACE2, Theme.SURFACE3, hov));
        g.outline(x, y, w, TILE_H, Theme.mix(Theme.LINE, Theme.alpha(Theme.BRAND, 0.7f), hov));
        if (on) g.rect(x, y, 2, TILE_H, m.accentColor());
        if (toggleable) Widgets.drawToggle(ui, m, tgx, tgy, on, true, toggleHot);
        moduleIcon(g, m, x + 10, y + 10, 24, runnable);
        int tx = x + 42;
        int right = toggleable ? tgx - 6 : x + w - 9;
        g.bold(g.clip(m.displayName(), right - tx), tx, y + 11, runnable ? Theme.TEXT : Theme.MUTED);
        int dw = x + w - 10 - tx;
        if (!runnable) {
            String tag = Lang.tr(m.status() == ModuleManager.Status.CONFLICT ? "core.module.providedShort" : "core.module.status.off");
            g.text(g.clip(tag, dw), tx, y + 25, Theme.WARN);
            ui.tooltip(m, x, y, w, TILE_H, m.statusReason());
        } else if (settingHit != null) {
            g.text(g.clip("› " + settingHit, dw), tx, y + 25, Theme.BRAND2);
        } else {
            String summaryKey = m.id() + ".summary";
            String summary = Lang.has(summaryKey) ? Lang.tr(summaryKey) : Lang.tr(m.descriptionKey());
            List<String> lines = Ui.wrap(g, summary, dw);
            int ly = y + 25;
            for (int k = 0; k < Math.min(2, lines.size()); k++) {
                String l = k == 1 && lines.size() > 2 ? g.clip(lines.get(1) + " …", dw) : lines.get(k);
                g.text(g.clip(l, dw), tx, ly, Theme.mix(Theme.DIM, Theme.MUTED, hov));
                ly += 10;
            }
            if (lines.size() > 2) ui.tooltip(m, x, y, w, TILE_H, summary);
        }
        if (toggled) ModuleManager.get().toggle(m.id());
        else if (clicked) open(m);
    }

    /** Search result that leads to the General tab. */
    private void generalTile(Ui ui, int x, int y, int w, String hit) {
        Gfx g = ui.g;
        boolean clicked = ui.clicked(x, y, w, TILE_H);
        boolean hot = ui.focusedLast() || ui.hover(x, y, w, TILE_H);
        float hov = ui.anim(GENERAL_TILE, 0f).to(hot ? 1f : 0f);
        g.rect(x, y, w, TILE_H, Theme.mix(Theme.SURFACE2, Theme.SURFACE3, hov));
        g.outline(x, y, w, TILE_H, Theme.mix(Theme.LINE, Theme.alpha(Theme.BRAND, 0.7f), hov));
        Widgets.iconTile(g, Icons.GEAR, x + 10, y + 10, 24, 2, Theme.MUTED);
        int tx = x + 42;
        g.bold(g.clip(Lang.tr("core.tab.general"), x + w - 10 - tx), tx, y + 11, Theme.TEXT);
        String line = hit.isEmpty() ? Lang.tr("core.general.description") : "› " + hit;
        g.text(g.clip(line, x + w - 10 - tx), tx, y + 25, hit.isEmpty() ? Theme.DIM : Theme.BRAND2);
        if (clicked && ClientCompat.screen() instanceof VibeyScreen s) s.showTab(VibeyTabs.GENERAL);
    }

    // ============================================================================================ module page

    private void open(VibeyModule m) {
        if (page == m) return;
        Panel old = page == null ? null : customPanels.get(page);
        if (old != null) old.onHide();
        page = m;
        lastPage = m;
        pageScroll.reset();
        if (ui != null) ui.clearFocus();
        if (m != null) {
            Panel p = panelOf(m);
            if (p != null) p.onShow();
        }
    }

    private Panel panelOf(VibeyModule m) {
        if (!customPanels.containsKey(m)) customPanels.put(m, m.createSettingsPanel());
        return customPanels.get(m);
    }

    private void drawPage(Ui ui, VibeyModule m, int x, int y, int w, int h) {
        Gfx g = ui.g;
        int pad = Theme.PAD + 4;
        // toolbar: back, breadcrumb, switch
        g.rect(x, y, w, BAR_H, Theme.PANEL);
        g.hline(x, y + BAR_H - 1, w, Theme.LINE);
        String back = Lang.tr("core.tab.modules");
        int bw = g.width(back) + 24;
        boolean bc = ui.clicked(x + pad - 4, y + 5, bw, 20);
        boolean bh = ui.focusedLast() || ui.hover(x + pad - 4, y + 5, bw, 20);
        if (bh) g.rect(x + pad - 4, y + 5, bw, 20, Theme.SURFACE3);
        Widgets.arrow(g, x + pad + 2, y + 12, false, bh ? Theme.TEXT : Theme.MUTED);
        g.text(back, x + pad + 10, y + 11, bh ? Theme.TEXT : Theme.MUTED);
        int crumbX = x + pad - 4 + bw + 6;
        g.text("/", crumbX, y + 11, Theme.DIM);
        boolean runnable = m.status().loadable();
        boolean toggle = runnable && m.canToggle();
        String state = Lang.tr(m.isEnabled() ? "core.module.status.enabled" : "core.module.status.disabled");
        int tgx = x + w - pad - Widgets.TOGGLE_W;
        int stateW = toggle ? g.width(state) + 8 : 0;
        g.bold(g.clip(m.displayName(), tgx - stateW - crumbX - 20), crumbX + 10, y + 11, Theme.TEXT);
        if (toggle) {
            g.textRight(state, tgx - 8, y + 11, m.isEnabled() ? Theme.OK : Theme.DIM);
            if (Widgets.toggle(ui, PAGE_TOGGLE, tgx, y + 9, m.isEnabled(), true)) ModuleManager.get().toggle(m.id());
        }
        if (bc) {
            open(null);
            return;
        }

        int top = y + BAR_H;
        int bodyH = h - BAR_H;
        Panel custom = panelOf(m);
        if (custom != null && runnable) {
            custom.draw(ui, x, top, w, bodyH);
            return;
        }
        int cx = x + pad, cw = w - pad * 2;
        int cy = pageScroll.begin(ui, x, top, w, bodyH) + 12;
        cy = intro(ui, m, cx, cy, cw);
        cy = SettingsView.draw(ui, m.settings(), cx, cy, cw, runnable && m.isEnabled());
        pageScroll.end(ui, cy + pad);
    }

    /**
     * The intro of a module page: big icon, description, the module's buttons and why it cannot run. Public for
     * custom module pages that want the same look.
     */
    public static int intro(Ui ui, VibeyModule m, int x, int y, int w) {
        Gfx g = ui.g;
        boolean runnable = m.status().loadable();
        moduleIcon(g, m, x, y, 32, runnable);
        int tx = x + 44;
        List<String> lines = Ui.wrap(g, Lang.tr(m.descriptionKey()), w - 44);
        int ly = y + (lines.size() == 1 ? 12 : lines.size() == 2 ? 6 : 1);
        for (int k = 0; k < Math.min(3, lines.size()); k++) {
            g.text(lines.get(k), tx, ly, Theme.MUTED);
            ly += 10;
        }
        y += Math.max(32, ly - y) + 10;
        List<ActionSetting> actions = SettingsView.actions(m.settings());
        if (!actions.isEmpty() && runnable) {
            int bx = x;
            boolean first = true;
            for (ActionSetting a : actions) {
                String label = SettingsView.actionLabel(a);
                int bw = g.width(label) + 20;
                if (bx + bw > x + w && bx > x) {
                    bx = x;
                    y += 20;
                }
                boolean usable = m.isEnabled();
                Widgets.Style style = !usable ? Widgets.Style.DISABLED : first ? Widgets.Style.PRIMARY : Widgets.Style.SECONDARY;
                if (Widgets.button(ui, a, bx, y, bw, 16, label, style)) a.run();
                if (!usable) ui.tooltip(a, bx, y, bw, 16, Lang.tr("core.module.turnOnFirst"));
                bx += bw + 6;
                first = false;
            }
            y += 16 + 12;
        }
        String reason = m.statusReason();
        if (reason != null) {
            List<String> rl = Ui.wrap(g, reason, w - 16);
            int rh = rl.size() * 10 + 10;
            g.rect(x, y, w, rh, Theme.alpha(Theme.WARN, 0.10f));
            g.vline(x, y, rh, Theme.WARN);
            int ry = y + 6;
            for (String l : rl) {
                g.text(l, x + 8, ry, Theme.WARN);
                ry += 10;
            }
            y += rh + 10;
        }
        // e.g. "Lyrics: provided by the installed Vibey Lyrics" (parts of a module that are off)
        for (String notice : m.notices()) {
            List<String> nl = Ui.wrap(g, notice, w - 16);
            int nh = nl.size() * 10 + 10;
            g.rect(x, y, w, nh, Theme.alpha(Theme.WARN, 0.10f));
            g.vline(x, y, nh, Theme.WARN);
            int ny = y + 6;
            for (String l : nl) {
                g.text(l, x + 8, ny, Theme.WARN);
                ny += 10;
            }
            y += nh + 10;
        }
        return y;
    }

    // ============================================================================================ keys

    /** Type-to-search: the first printable key focuses the search field (on a module page: back to the grid). */
    @Override
    public boolean typed(int codepoint) {
        if (codepoint <= 32 || Character.isISOControl(codepoint)) return false;
        if (page != null) open(null);
        if (searchBox == null) return false;
        net.minecraft.client.gui.screens.Screen screen = ClientCompat.screen();
        if (screen != null) screen.setFocused(searchBox);
        searchBox.setFocused(true);
        searchBox.insertText(new String(Character.toChars(codepoint)));
        return true;
    }

    @Override
    public boolean key(int keyCode) {
        if (keyCode != Keys.ESCAPE) return false;
        // Esc: module page → grid, then clear the search, then the menu closes
        if (page != null) {
            open(null);
            return true;
        }
        if (!query.isEmpty()) {
            query = "";
            if (searchBox != null) searchBox.setValue("");
            gridScroll.reset();
            return true;
        }
        return false;
    }

    @Override
    public void onShow() {
        if (page != null) {
            Panel p = panelOf(page);
            if (p != null) p.onShow();
        }
    }

    @Override
    public void onHide() {
        Panel p = page == null ? null : customPanels.get(page);
        if (p != null) p.onHide();
    }
}
