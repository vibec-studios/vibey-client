package dev.henny.vibeyclient.ui.hud;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.Anim;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;
import net.minecraft.client.gui.components.EditBox;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The HUD editor's panels: the widget library (left), the right-click menus, the dock's dropdowns (templates,
 * share), the style popover with its colour picker, the import dialog, the first-run intro and toasts. Immediate mode
 * on the screen's {@link Ui}; every panel registers its background last so clicks on it never reach the canvas.
 */
final class HudEditorPanels {
    enum Menu { NONE, CONTEXT, CANVAS, TEMPLATES, SHARE }

    private static final int LIB_W = 170;
    private static final int POP_W = 204;
    private static final int ROW = 17;

    private final HudEditorScreen s;

    // library
    private boolean library;
    private final Anim libAnim = new Anim(0f);
    private final ScrollArea libScroll = new ScrollArea();

    // menus
    private Menu menu = Menu.NONE;
    private int menuX, menuY;
    private boolean menuUp;
    private final Anim menuAnim = new Anim(0f);
    private int menuW, menuH;

    // style popover
    private boolean style;
    private boolean global;
    private int page;
    private final Anim popAnim = new Anim(0f);
    private int popX, popY, popH;
    private int picker = -1; // 0 background, 1 text, 2 accent
    private final float[] hsv = new float[3];
    private boolean hexEditing;

    // dialogs
    private boolean importDialog;
    private String importText = "";
    private String importError;
    private List<Path> files = List.of();
    private int dlgX, dlgY, dlgW, dlgH;

    // intro
    private int intro = -1;
    private final Anim introAnim = new Anim(0f);
    private long introStepAt;

    // toast
    private String toast;
    private long toastAt;

    private final List<HudElement> rows = new ArrayList<>();

    HudEditorPanels(HudEditorScreen s) {
        this.s = s;
    }

    // ============================================================================================ caches (no per-frame allocation)

    private final java.util.HashMap<String, Object[]> fmtCache = new java.util.HashMap<>();
    private final java.util.IdentityHashMap<Object, Object[]> wrapCache = new java.util.IdentityHashMap<>();
    private final java.util.IdentityHashMap<Object, Object[]> keys = new java.util.IdentityHashMap<>();

    /** {@code Lang.tr(key, arg)}, cached until the argument or the language changes. */
    private String trc(String key, Object arg) {
        String pattern = Lang.tr(key);
        Object[] c = fmtCache.get(key);
        if (c == null || c[0] != pattern || !c[1].equals(arg)) {
            c = new Object[] {pattern, arg, Lang.tr(key, arg)};
            fmtCache.put(key, c);
        }
        return (String) c[2];
    }

    /** Wrapped lines of {@code text} (cached per text instance and width). */
    @SuppressWarnings("unchecked")
    private List<String> wrap(Gfx g, String text, int w) {
        Object[] c = wrapCache.get(text);
        if (c == null || (int) c[0] != w) {
            c = new Object[] {w, Ui.wrap(g, text, w)};
            wrapCache.put(text, c);
        }
        return (List<String>) c[1];
    }

    /** Stable keys (for animations and tooltips) belonging to an object. */
    private Object key(Object owner, int i) {
        Object[] k = keys.get(owner);
        if (k == null) {
            k = new Object[] {new Object(), new Object(), new Object(), new Object()};
            keys.put(owner, k);
        }
        return k[i];
    }

    // ============================================================================================ state

    boolean libraryOpen() {
        return library;
    }

    void toggleLibrary() {
        library = !library;
        if (library) menu = Menu.NONE;
    }

    int libraryRight() {
        return Math.round(LIB_W * libAnim.value());
    }

    Menu menu() {
        return menu;
    }

    boolean modal() {
        return intro >= 0 || importDialog;
    }

    void toast(String text) {
        toast = text;
        toastAt = System.currentTimeMillis();
    }

    void selectionChanged() {
        if (style && s.primary() == null) closeStyle();
        picker = -1;
    }

    /** True if a panel covers the point (no canvas hover under it). */
    boolean covers(int mx, int my) {
        if (modal()) return true;
        if (library && mx < libraryRight()) return true;
        if (menu != Menu.NONE && mx >= menuX && mx < menuX + menuW && my >= menuY && my < menuY + menuH) return true;
        return style && mx >= popX && mx < popX + POP_W && my >= popY && my < popY + popH;
    }

    /** A click on the canvas closes the open menu (and is used up by that). */
    boolean closeTransient() {
        if (menu != Menu.NONE) {
            menu = Menu.NONE;
            return true;
        }
        return false;
    }

    boolean escape() {
        if (intro >= 0) {
            closeIntro();
            return true;
        }
        if (importDialog) {
            importDialog = false;
            return true;
        }
        if (menu != Menu.NONE) {
            menu = Menu.NONE;
            return true;
        }
        if (picker >= 0) {
            picker = -1;
            return true;
        }
        if (style) {
            closeStyle();
            return true;
        }
        if (library) {
            library = false;
            return true;
        }
        return false;
    }

    boolean enter() {
        if (importDialog) {
            doImport();
            return true;
        }
        if (intro >= 0) {
            nextIntro();
            return true;
        }
        return false;
    }

    void toggleMenu(Menu m, int x, int dockY, boolean dockTop) {
        if (menu == m) {
            menu = Menu.NONE;
            return;
        }
        menu = m;
        menuAnim.snap(0f);
        if (m == Menu.TEMPLATES) templateList = HudTemplates.all();
        menuX = x;
        menuUp = !dockTop;
        menuY = dockTop ? dockY + s.dockH + 4 : dockY - 4; // final y once the size is known
    }

    void openContextMenu(int x, int y) {
        menu = Menu.CONTEXT;
        menuAnim.snap(0f);
        menuX = x + 2;
        menuY = y + 2;
        menuUp = false;
    }

    void openCanvasMenu(int x, int y) {
        menu = Menu.CANVAS;
        menuAnim.snap(0f);
        menuX = x + 2;
        menuY = y + 2;
        menuUp = false;
    }

    void openStyle(int pageIndex) {
        style = true;
        popAnim.snap(0f);
        HudElement p = s.primary();
        page = p != null && !p.styleable() ? 2 : pageIndex;
        global = false;
        picker = -1;
        menu = Menu.NONE;
    }

    void closeStyle() {
        style = false;
        picker = -1;
    }

    void openIntro() {
        intro = 0;
        introAnim.snap(0f);
        introStepAt = System.currentTimeMillis();
    }

    void closeIntro() {
        intro = -1;
        HudLayout.setFlag("intro", true);
    }

    private void nextIntro() {
        if (intro >= 2) closeIntro();
        else {
            intro++;
            introStepAt = System.currentTimeMillis();
        }
    }

    void testOpen(String what, int x, int y) {
        switch (what) {
            case "library" -> library = true;
            case "style" -> openStyle(0);
            case "styleText" -> openStyle(1);
            case "styleSize" -> openStyle(2);
            case "picker" -> {
                openStyle(1);
                openPicker(1);
            }
            case "context" -> openContextMenu(x, y);
            case "templates" -> toggleMenu(Menu.TEMPLATES, s.dockX + 120, s.dockY, false);
            case "share" -> toggleMenu(Menu.SHARE, s.dockX + 180, s.dockY, false);
            case "intro" -> openIntro();
            case "introNext" -> nextIntro();
            case "import" -> openImport();
            default -> {
            }
        }
    }

    // ============================================================================================ drawing

    void draw(Gfx g, int mx, int my, long now) {
        Ui ui = s.ui;
        float lib = libAnim.to(library ? 1f : 0f);
        s.highlight = null;
        if (lib > 0.01f) library(g, ui, lib, mx, my);
        if (style) popover(g, ui, mx, my);
        if (menu != Menu.NONE) menu(g, ui);
        toastDraw(g, now);
        if (importDialog) importDialog(g, ui);
        if (intro >= 0) intro(g, ui, now);
    }

    // ============================================================================================ library

    private static final HudElement.Category[] CATS = HudElement.Category.values();
    private static final String[] CAT_KEYS = new String[CATS.length];

    static {
        for (HudElement.Category c : CATS) CAT_KEYS[c.ordinal()] = "core.hud.cat." + c.name().toLowerCase(java.util.Locale.ROOT);
    }

    private void library(Gfx g, Ui ui, float t, int mx, int my) {
        int w = LIB_W;
        int x = Math.round(-w + w * t);
        int h = s.height;
        g.rect(x, 0, w, h, 0xF40B0B0E);
        g.rect(x + w - 1, 0, 1, h, Theme.LINE2);
        int cx = x + 10;
        g.bold(Lang.tr("core.hud.library.title"), cx, 10, Theme.TEXT);
        int on = 0;
        for (int i = 0; i < s.available.size(); i++) if (!HudLayout.hidden(s.available.get(i))) on++;
        g.text(trc("core.hud.library.onScreen", on), cx, 22, Theme.DIM);
        if (s.iconButton(g, "lib-close", x + w - 24, 6, Icons.CLOSE, true)) library = false;
        int top = 36;
        g.rect(x, top - 1, w - 1, 1, Theme.LINE);
        int y = libScroll.begin(ui, x, top, w - 1, h - top);
        for (HudElement.Category c : CATS) {
            rows.clear();
            for (int i = 0; i < s.available.size(); i++) {
                HudElement e = s.available.get(i);
                if (e.category() == c) rows.add(e);
            }
            if (rows.isEmpty()) continue;
            y += 6;
            Widgets.caption(g, Lang.tr(CAT_KEYS[c.ordinal()]), cx, y);
            y += 12;
            for (HudElement e : rows) y = libraryRow(g, ui, e, x, y, w - 1, mx, my);
        }
        y += 8;
        String hint = Lang.tr("core.hud.library.hint");
        for (String line : wrap(g, hint, w - 22)) {
            g.text(line, cx, y, Theme.DIM);
            y += 10;
        }
        y += 6;
        libScroll.end(ui, y);
        ui.clickedNoFocus(x, 0, w, h);
    }

    private int libraryRow(Gfx g, Ui ui, HudElement e, int x, int y, int w, int mx, int my) {
        int h = 26;
        boolean shown = !HudLayout.hidden(e);
        boolean disabled = HudLayout.serverDisabled(e);
        boolean hot = ui.hover(x, y, w, h);
        float hov = ui.anim(e, 3, 0f).to(hot ? 1f : 0f);
        if (hot && shown) s.highlight = e;
        int tgx = x + w - 10 - Widgets.TOGGLE_W, tgy = y + 7;
        // controls first (they win the click), then the row
        boolean toggled;
        if (e.isCopy()) {
            toggled = false;
            if (s.iconButton(g, key(e, 0), tgx + Widgets.TOGGLE_W - 18, y + 4, HudIcons.TRASH, true)) {
                s.checkpoint(null);
                HudLayout.removeCopy(e);
                s.selection.remove(e);
                return y + h;
            }
            ui.tooltip(key(e, 0), tgx + Widgets.TOGGLE_W - 18, y + 4, 18, 18, Lang.tr("core.hud.library.remove"));
        } else {
            toggled = Widgets.toggle(ui, e, tgx, tgy, shown, true);
            if (e.duplicable()) {
                int px = tgx - 21;
                if (s.iconButton(g, key(e, 1), px, y + 4, HudIcons.PLUS, true)) {
                    if (!shown) s.addToCanvas(e);
                    else {
                        s.selectOnly(e);
                        s.duplicateSelection();
                    }
                }
                ui.tooltip(key(e, 1), px, y + 4, 18, 18, Lang.tr("core.hud.library.add"));
            }
        }
        boolean rowClick = ui.clicked(x, y, w, h);
        g.rect(x, y, w, h, Theme.alpha(Theme.SURFACE3, hov * 0.8f));
        if (s.isSelected(e)) g.rect(x, y, 2, h, Theme.BRAND);
        int icon = shown ? Theme.BRAND : Theme.DIM;
        String ic = e.icon() != null ? e.icon() : HudIcons.MEDIA;
        Widgets.iconTile(g, ic, x + 10, y + 5, 16, 1, disabled ? Theme.WARN : icon);
        int tx = x + 32;
        int maxW = tgx - tx - (e.duplicable() && !e.isCopy() ? 26 : 6);
        g.text(g.clip(e.displayName(), maxW), tx, y + 5, shown ? Theme.TEXT : Theme.MUTED);
        String sub = disabled ? Lang.tr("core.hud.serverDisabled")
                : e.descriptionKey() != null ? Lang.tr(e.descriptionKey()) : null;
        if (sub != null) g.text(g.clip(sub, maxW), tx, y + 15, disabled ? Theme.WARN : Theme.DIM);
        if (disabled) ui.tooltip(key(e, 2), x, y, w, h, ServerRulesText.message());
        if (toggled) {
            if (shown) {
                s.checkpoint(null);
                HudLayout.setHidden(e, true);
                s.selection.remove(e);
            } else {
                s.addToCanvas(e);
            }
        } else if (rowClick) {
            if (!shown) s.addToCanvas(e);
            else s.selectOnly(e);
        }
        return y + h;
    }

    /** The server's message for disabled widgets (or a default line). */
    static final class ServerRulesText {
        static String message() {
            String m = dev.henny.vibeyclient.api.ServerRules.message();
            return m != null ? Lang.tr("core.hud.serverDisabled.tipWith", m) : Lang.tr("core.hud.serverDisabled.tip");
        }
    }

    // ============================================================================================ menus

    private static final int ITEM_H = 15;

    private void menu(Gfx g, Ui ui) {
        // measure
        int n = 0;
        int w = 0;
        HudElement p = s.primary();
        switch (menu) {
            case CONTEXT -> {
                if (p == null) {
                    menu = Menu.NONE;
                    return;
                }
                boolean anyStyle = anyStyleable();
                boolean anyDup = false;
                boolean anyCopy = false;
                for (HudElement e : s.selection) {
                    anyDup |= e.duplicable();
                    anyCopy |= e.isCopy();
                }
                n = 0;
                w = Math.max(w, itemW(g, anyStyle ? "core.hud.menu.style" : "core.hud.menu.size", "S"));
                n++;
                if (anyDup) { w = Math.max(w, itemW(g, "core.hud.menu.duplicate", "Ctrl+D")); n++; }
                w = Math.max(w, itemW(g, anyCopy && s.selection.size() == 1 ? "core.hud.menu.remove" : "core.hud.menu.hide", "Del"));
                n++;
                w = Math.max(w, itemW(g, "core.hud.menu.reset", "R"));
                n++;
                String headText = s.selection.size() > 1 ? trc("core.hud.selected", s.selection.size()) : p.displayName();
                w = Math.max(w, Math.min(180, g.width(headText) + 14));
                menuW = w;
                menuH = n * ITEM_H + 6 + 13;
                place();
                float a = menuAnim.to(1f);
                frame(g, a);
                int y = menuY + 3;
                String head = s.selection.size() > 1 ? trc("core.hud.selected", s.selection.size()) : p.displayName();
                g.text(g.clip(head, menuW - 12), menuX + 6, y + 2, Theme.DIM);
                y += 13;
                if (item(g, ui, "m-style", y, HudIcons.STYLE, anyStyle ? "core.hud.menu.style" : "core.hud.menu.size", null)) {
                    menu = Menu.NONE;
                    openStyle(anyStyle ? 0 : 2);
                }
                y += ITEM_H;
                if (anyDup) {
                    if (item(g, ui, "m-dup", y, HudIcons.COPY, "core.hud.menu.duplicate", "Ctrl+D")) {
                        menu = Menu.NONE;
                        s.duplicateSelection();
                    }
                    y += ITEM_H;
                }
                boolean remove = anyCopy && s.selection.size() == 1;
                if (item(g, ui, "m-hide", y, remove ? HudIcons.TRASH : HudIcons.EYE_OFF, remove ? "core.hud.menu.remove" : "core.hud.menu.hide", "Del")) {
                    menu = Menu.NONE;
                    s.hideSelection();
                }
                y += ITEM_H;
                if (item(g, ui, "m-reset", y, HudIcons.RESET, "core.hud.menu.reset", "R")) {
                    menu = Menu.NONE;
                    s.resetSelection();
                }
            }
            case CANVAS -> {
                w = Math.max(itemW(g, "core.hud.menu.widgets", null), itemW(g, "core.hud.menu.selectAll", "Ctrl+A"));
                w = Math.max(w, itemW(g, "core.hud.menu.globalStyle", null));
                w = Math.max(w, itemW(g, "core.hud.menu.undo", "Ctrl+Z"));
                menuW = w;
                menuH = 4 * ITEM_H + 6;
                place();
                frame(g, menuAnim.to(1f));
                int y = menuY + 3;
                if (item(g, ui, "c-lib", y, HudIcons.LIBRARY, "core.hud.menu.widgets", null)) {
                    menu = Menu.NONE;
                    library = true;
                }
                y += ITEM_H;
                if (item(g, ui, "c-all", y, HudIcons.TEMPLATE, "core.hud.menu.selectAll", "Ctrl+A")) {
                    menu = Menu.NONE;
                    s.selection.clear();
                    s.selection.addAll(s.canvas);
                }
                y += ITEM_H;
                if (item(g, ui, "c-global", y, HudIcons.STYLE, "core.hud.menu.globalStyle", null)) {
                    menu = Menu.NONE;
                    if (s.primary() == null && !s.canvas.isEmpty()) s.selectOnly(firstStyleable());
                    if (s.primary() != null) {
                        openStyle(0);
                        global = true;
                    }
                }
                y += ITEM_H;
                if (item(g, ui, "c-undo", y, HudIcons.UNDO, "core.hud.menu.undo", "Ctrl+Z")) {
                    menu = Menu.NONE;
                    s.undo();
                }
            }
            case TEMPLATES -> templatesMenu(g, ui);
            case SHARE -> shareMenu(g, ui);
            default -> {
            }
        }
        if (menu != Menu.NONE) ui.clickedNoFocus(menuX, menuY, menuW, menuH);
    }

    private HudElement firstStyleable() {
        for (HudElement e : s.canvas) if (e.styleable()) return e;
        return null;
    }

    private boolean anyStyleable() {
        for (HudElement e : s.selection) if (e.styleable()) return true;
        return false;
    }

    private void place() {
        if (menu == Menu.TEMPLATES || menu == Menu.SHARE) {
            int y = menuUp ? s.dockY - 4 - menuH : s.dockY + s.dockH + 4;
            menuY = y;
        }
        menuX = Math.max(2, Math.min(s.width - menuW - 2, menuX));
        menuY = Math.max(2, Math.min(s.height - menuH - 2, menuY));
    }

    private void frame(Gfx g, float a) {
        int off = Math.round((1f - a) * 4);
        g.rect(menuX + 2, menuY + 2 + off, menuW, menuH, 0x60000000);
        g.rect(menuX, menuY + off, menuW, menuH, 0xFA0B0B0E);
        g.outline(menuX, menuY + off, menuW, menuH, Theme.LINE2);
    }

    private int itemW(Gfx g, String key, String shortcut) {
        return 22 + g.width(Lang.tr(key)) + (shortcut != null ? g.width(shortcut) + 16 : 8) + 6;
    }

    private boolean item(Gfx g, Ui ui, Object key, int y, String icon, String labelKey, String shortcut) {
        int x = menuX + 1, w = menuW - 2;
        boolean clicked = ui.clicked(x, y, w, ITEM_H);
        boolean hot = ui.hover(x, y, w, ITEM_H) || ui.focusedLast();
        float hov = ui.anim(key, 1, 0f).to(hot ? 1f : 0f);
        if (hov > 0.01f) g.rect(x, y, w, ITEM_H, Theme.alpha(Theme.BRAND, 0.22f * hov));
        if (hot) g.rect(x, y, 1, ITEM_H, Theme.BRAND);
        if (icon != null) Icons.draw(g, icon, x + 6, y + 3, 1, hot ? Theme.BRAND_HOVER : Theme.MUTED);
        g.text(Lang.tr(labelKey), x + 21, y + 4, hot ? Theme.TEXT : Theme.mix(Theme.MUTED, Theme.TEXT, 0.6f));
        if (shortcut != null) g.textRight(shortcut, x + w - 6, y + 4, Theme.DIM);
        return clicked;
    }

    private List<HudTemplates.Template> templateList = List.of();

    private void templatesMenu(Gfx g, Ui ui) {
        List<HudTemplates.Template> list = templateList;
        int w = 150;
        for (HudTemplates.Template t : list) {
            w = Math.max(w, g.width(Lang.tr(t.labelKey())) + 30);
            w = Math.max(w, Math.min(280, g.width(Lang.tr(t.descKey())) + 38));
        }
        int ih = 24;
        menuW = w;
        menuH = 16 + Math.max(1, list.size()) * ih + 4;
        place();
        frame(g, menuAnim.to(1f));
        g.text(Lang.tr("core.hud.templates.title"), menuX + 7, menuY + 5, Theme.DIM);
        int y = menuY + 16;
        if (list.isEmpty()) g.text(Lang.tr("core.hud.templates.none"), menuX + 7, y + 6, Theme.MUTED);
        for (HudTemplates.Template t : list) {
            int x = menuX + 1, iw = menuW - 2;
            boolean clicked = ui.clicked(x, y, iw, ih);
            boolean hot = ui.hover(x, y, iw, ih) || ui.focusedLast();
            float hov = ui.anim(t, 1, 0f).to(hot ? 1f : 0f);
            if (hov > 0.01f) g.rect(x, y, iw, ih, Theme.alpha(Theme.BRAND, 0.22f * hov));
            if (hot) g.rect(x, y, 1, ih, Theme.BRAND);
            thumb(g, t, x + 6, y + 4, hot);
            g.text(Lang.tr(t.labelKey()), x + 30, y + 3, Theme.TEXT);
            g.text(g.clip(Lang.tr(t.descKey()), iw - 34), x + 30, y + 13, Theme.DIM);
            if (clicked) {
                menu = Menu.NONE;
                try {
                    s.applyState(t.state().get(), true, null);
                    toast(Lang.tr("core.hud.templates.applied", Lang.tr(t.labelKey())));
                } catch (RuntimeException e) {
                    toast(Lang.tr("core.hud.templates.failed"));
                }
            }
            y += ih;
        }
    }

    /** A tiny 20 × 14 preview of a template (boxes at their anchors). */
    private void thumb(Gfx g, HudTemplates.Template t, int x, int y, boolean hot) {
        g.rect(x, y, 20, 15, 0xFF16161C);
        g.outline(x, y, 20, 15, hot ? Theme.BRAND : Theme.LINE2);
        int h = t.id().hashCode();
        int c = hot ? Theme.BRAND_HOVER : Theme.MUTED;
        switch (t.id()) {
            case "pvp" -> {
                g.rect(x + 2, y + 2, 5, 2, c);
                g.rect(x + 2, y + 5, 4, 2, c);
                g.rect(x + 14, y + 9, 4, 4, c);
                g.rect(x + 8, y + 2, 4, 1, c);
            }
            case "minimal" -> {
                g.rect(x + 2, y + 2, 4, 2, c);
                g.rect(x + 15, y + 2, 3, 2, c);
            }
            case "building" -> {
                g.rect(x + 2, y + 2, 7, 4, c);
                g.rect(x + 13, y + 2, 5, 2, c);
                g.rect(x + 7, y + 2, 6, 1, c);
            }
            case "streamer" -> {
                g.rect(x + 2, y + 10, 6, 3, c);
                g.rect(x + 13, y + 2, 5, 2, c);
                g.rect(x + 2, y + 2, 6, 2, Theme.alpha(c, 0.6f));
            }
            default -> {
                g.rect(x + 2 + (h & 7), y + 2 + ((h >> 3) & 5), 5, 2, c);
                g.rect(x + 2 + ((h >> 6) & 7), y + 8, 6, 2, c);
            }
        }
    }

    private void shareMenu(Gfx g, Ui ui) {
        int w = Math.max(itemW(g, "core.hud.share.copy", null), itemW(g, "core.hud.share.paste", null));
        w = Math.max(w, itemW(g, "core.hud.share.saveFile", null));
        w = Math.max(w, itemW(g, "core.hud.share.folder", null));
        menuW = w;
        menuH = 4 * ITEM_H + 6;
        place();
        frame(g, menuAnim.to(1f));
        int y = menuY + 3;
        if (item(g, ui, "s-copy", y, HudIcons.COPY, "core.hud.share.copy", null)) {
            menu = Menu.NONE;
            try {
                String code = HudShare.exportCode();
                ClientCompat.setClipboard(code);
                toast(Lang.tr("core.hud.share.copied", code.length()));
            } catch (RuntimeException e) {
                toast(Lang.tr("core.hud.share.error.write"));
            }
        }
        y += ITEM_H;
        if (item(g, ui, "s-paste", y, HudIcons.PASTE, "core.hud.share.paste", null)) {
            menu = Menu.NONE;
            openImport();
        }
        y += ITEM_H;
        if (item(g, ui, "s-save", y, HudIcons.FILE, "core.hud.share.saveFile", null)) {
            menu = Menu.NONE;
            try {
                Path f = HudShare.exportFile();
                toast(Lang.tr("core.hud.share.saved", f.getFileName().toString()));
            } catch (Exception e) {
                toast(Lang.tr("core.hud.share.error.write"));
            }
        }
        y += ITEM_H;
        if (item(g, ui, "s-folder", y, HudIcons.FOLDER, "core.hud.share.folder", null)) {
            menu = Menu.NONE;
            openFolder();
        }
    }

    private static void openFolder() {
        try {
            java.nio.file.Files.createDirectories(HudShare.folder());
            ClientCompat.openFolder(HudShare.folder());
        } catch (Exception e) {
            dev.henny.vibeyclient.Vibey.LOG.warn("Could not open {}", HudShare.folder(), e);
        }
    }

    // ============================================================================================ import

    private void openImport() {
        importDialog = true;
        importError = null;
        String clip = ClientCompat.clipboard().strip();
        importText = clip.regionMatches(true, 0, "VHUD", 0, 4) && clip.length() < HudShare.MAX_CODE ? clip : "";
        files = HudShare.files();
        menu = Menu.NONE;
    }

    private void doImport() {
        try {
            HudLayout.State st = HudShare.importCode(importText);
            s.applyState(st, true, "core.hud.share.imported");
            importDialog = false;
        } catch (HudShare.BadLayout e) {
            importError = Lang.tr(e.key);
        }
    }

    void importFile(Path f) {
        try {
            HudLayout.State st = HudShare.importFile(f);
            s.applyState(st, true, null);
            toast(Lang.tr("core.hud.share.loaded", f.getFileName().toString()));
            importDialog = false;
        } catch (HudShare.BadLayout e) {
            if (importDialog) importError = Lang.tr(e.key);
            else toast(Lang.tr(e.key));
        }
    }

    private void importDialog(Gfx g, Ui ui) {
        g.rect(0, 0, s.width, s.height, 0x90000000);
        dlgW = Math.min(300, s.width - 20);
        int listH = Math.min(5, Math.max(1, files.size())) * 14;
        dlgH = 140 + listH;
        dlgX = (s.width - dlgW) / 2;
        dlgY = Math.max(6, (s.height - dlgH) / 2);
        int x = dlgX, y = dlgY;
        g.rect(x + 3, y + 3, dlgW, dlgH, 0x70000000);
        g.rect(x, y, dlgW, dlgH, 0xFF0B0B0E);
        g.outline(x, y, dlgW, dlgH, Theme.LINE2);
        g.rect(x, y, dlgW, 1, Theme.BRAND);
        g.bold(Lang.tr("core.hud.import.title"), x + 10, y + 9, Theme.TEXT);
        g.text(Lang.tr("core.hud.import.hint"), x + 10, y + 21, Theme.DIM);
        int fy = y + 34;
        int pasteW = g.width(Lang.tr("core.hud.import.paste")) + 14;
        EditBox box = Widgets.textField(ui, "import-code", x + 10, fy, dlgW - 24 - pasteW, importText, HudShare.MAX_CODE, v -> {
            importText = v;
            importError = null;
        });
        if (!box.isFocused() && importText.isEmpty()) box.setFocused(true);
        if (Widgets.button(ui, "import-paste", x + dlgW - 10 - pasteW, fy, pasteW, 16, Lang.tr("core.hud.import.paste"), Widgets.Style.SECONDARY)) {
            importText = ClientCompat.clipboard().strip();
            box.setValue(importText);
            importError = null;
        }
        int ey = fy + 20;
        if (importError != null) {
            Icons.draw(g, Icons.WARN, x + 10, ey, 1, Theme.DANGER);
            g.text(g.clip(importError, dlgW - 34), x + 22, ey + 1, Theme.DANGER);
        }
        int ly = ey + 16;
        Widgets.caption(g, Lang.tr("core.hud.import.files"), x + 10, ly);
        ly += 11;
        if (files.isEmpty()) {
            g.text(Lang.tr("core.hud.import.noFiles"), x + 10, ly + 2, Theme.MUTED);
        }
        for (int i = 0; i < Math.min(5, files.size()); i++) {
            Path f = files.get(i);
            int rw = dlgW - 20;
            boolean hot = ui.hover(x + 10, ly, rw, 14);
            if (ui.clicked(x + 10, ly, rw, 14)) importFile(f);
            if (hot) g.rect(x + 10, ly, rw, 14, Theme.alpha(Theme.BRAND, 0.2f));
            Icons.draw(g, HudIcons.FILE, x + 13, ly + 2, 1, hot ? Theme.BRAND_HOVER : Theme.MUTED);
            g.text(g.clip(f.getFileName().toString(), rw - 24), x + 26, ly + 3, hot ? Theme.TEXT : Theme.MUTED);
            ly += 14;
        }
        int by = y + dlgH - 26;
        g.text(g.clip(Lang.tr("core.hud.import.drop"), dlgW - 20), x + 10, by - 14, Theme.DIM);
        String imp = Lang.tr("core.hud.import.apply"), cancel = Lang.tr("core.hud.cancel"), folder = Lang.tr("core.hud.import.folder");
        int wi = g.width(imp) + 16, wc = g.width(cancel) + 16, wf = g.width(folder) + 16;
        int bx = x + dlgW - 10 - wi;
        if (Widgets.button(ui, "import-go", bx, by, wi, 16, imp, importText.isBlank() ? Widgets.Style.DISABLED : Widgets.Style.PRIMARY)) doImport();
        bx -= wc + 4;
        if (Widgets.button(ui, "import-cancel", bx, by, wc, 16, cancel, Widgets.Style.GHOST)) importDialog = false;
        bx -= wf + 4;
        if (Widgets.button(ui, "import-folder", bx, by, wf, 16, folder, Widgets.Style.SECONDARY)) openFolder();
        ui.clickedNoFocus(0, 0, s.width, s.height);
    }

    // ============================================================================================ style popover

    private static final int[] BG_COLORS = {0x0B0B0E, 0x000000, 0x1B1530, 0x0D2622, 0x1C2230, 0x2A1220, 0xF4F4F5};
    private static final int[] TEXT_COLORS = {0xF4F4F5, 0xA1A1AA, 0x8B6CFF, 0x3FE0C5, 0xFF5FA8, 0xFFD166, 0x7BE06B, 0x5AA9FF};
    private static final int[] ACCENT_COLORS = {0x8B6CFF, 0x3FE0C5, 0xFF5FA8, 0xFFD166, 0x5AA9FF, 0xFF5470, 0xF4F4F5};
    private static final String[] TABS = {"core.hud.style.tab.box", "core.hud.style.tab.text", "core.hud.style.tab.size"};

    private final HudLayout.Box popBox = new HudLayout.Box();

    private void popover(Gfx g, Ui ui, int mx, int my) {
        HudElement p = s.primary();
        if (p == null) {
            closeStyle();
            return;
        }
        boolean styled = anyStyleable();
        if (!styled) {
            global = false;
            page = 2;
        }
        float a = popAnim.to(1f);
        int w = POP_W;
        int h = measurePop(g, p, styled);
        popH = h;
        // beside the selection: right, else left, else below/above
        HudLayout.Box b = HudLayout.box(p, s.width, s.height, popBox);
        int x, y;
        if (b.x + b.w + 8 + w <= s.width - 4) x = Math.round(b.x + b.w + 8);
        else if (b.x - 8 - w >= 4) x = Math.round(b.x - 8 - w);
        else x = Math.round(b.x + b.w / 2 - w / 2f);
        y = Math.round(b.y);
        if (library && x < libraryRight() + 4) x = libraryRight() + 4;
        x = Math.max(4, Math.min(s.width - w - 4, x));
        y = Math.max(4, Math.min(s.height - h - 4, y));
        y += Math.round((1f - a) * 4);
        popX = x;
        popY = y;
        g.rect(x + 3, y + 3, w, h, 0x60000000);
        g.rect(x, y, w, h, 0xFA0B0B0E);
        g.outline(x, y, w, h, Theme.LINE2);
        g.rect(x, y, w, 1, Theme.BRAND);
        int cx = x + 8, cw = w - 16;
        int cy = y + 7;
        // header
        String title = global ? Lang.tr("core.hud.style.globalTitle")
                : s.selection.size() > 1 ? trc("core.hud.selected", s.selection.size()) : p.displayName();
        Icons.draw(g, global ? HudIcons.STYLE : p.icon() != null ? p.icon() : HudIcons.MEDIA, cx, cy, 1, Theme.BRAND);
        g.bold(g.clip(title, cw - 32), cx + 13, cy + 1, Theme.TEXT);
        if (s.iconButton(g, "pop-close", x + w - 22, y + 4, Icons.CLOSE, true)) {
            closeStyle();
            return;
        }
        cy += 16;
        if (styled) {
            int sel = Widgets.segmented(ui, "pop-scope", cx, cy, cw, scopeLabels(), global ? 1 : 0);
            if (sel >= 0) {
                global = sel == 1;
                picker = -1;
                if (global && page == 2) page = 0;
            }
            cy += 21;
            // tabs
            int tx = cx;
            int tabs = global ? 2 : 3;
            for (int i = 0; i < tabs; i++) {
                String l = Lang.tr(TABS[i]);
                int tw = g.width(l) + 12;
                boolean on = page == i;
                boolean hot = ui.hover(tx, cy, tw, 13);
                if (ui.clicked(tx, cy, tw, 13)) {
                    page = i;
                    picker = -1;
                }
                g.text(l, tx + 6, cy + 2, on ? Theme.TEXT : hot ? Theme.mix(Theme.MUTED, Theme.TEXT, 0.6f) : Theme.MUTED);
                if (on) g.rect(tx + 3, cy + 12, tw - 6, 1, Theme.BRAND);
                tx += tw;
            }
            g.rect(cx, cy + 13, cw, 1, Theme.LINE);
            cy += 19;
        }
        if (picker >= 0) {
            picker(g, ui, cx, cy, cw);
        } else if (page == 0) {
            boxPage(g, ui, p, cx, cy, cw);
        } else if (page == 1) {
            textPage(g, ui, p, cx, cy, cw);
        } else {
            sizePage(g, ui, p, cx, cy, cw, styled);
        }
        ui.clickedNoFocus(x, y, w, h);
    }

    private String[] scopeCache;
    private String scopeLang;
    private int scopeN;

    private String[] scopeLabels() {
        String lang = Lang.tr("core.hud.style.scope.global");
        int n = s.selection.size();
        if (scopeCache == null || scopeLang != lang || scopeN != n) {
            scopeLang = lang;
            scopeN = n;
            scopeCache = new String[] {n > 1 ? Lang.tr("core.hud.style.scope.many", n) : Lang.tr("core.hud.style.scope.widget"), lang};
        }
        return scopeCache;
    }

    private int measurePop(Gfx g, HudElement p, boolean styled) {
        int h = 7 + 16 + (styled ? 21 + 19 : 0);
        if (picker >= 0) return h + 104;
        if (page == 0) h += 5 * ROW + 4;
        else if (page == 1) h += 4 * ROW + 4;
        else {
            h += ROW + 4; // size
            if (styled && (p.hasVariants() || anyVariants())) h += ROW;
            h += 32; // anchor
            int opts = 0;
            for (HudOption o : p.options()) opts++;
            if (opts > 0 && s.selection.size() == 1) h += 14 + opts * ROW;
        }
        if (styled && !global && picker < 0 && page < 2) h += 22;
        if (global && picker < 0) h += 24;
        return h;
    }

    private boolean anyVariants() {
        for (HudElement e : s.selection) if (e.hasVariants()) return true;
        return false;
    }

    /** The style the popover shows (global, or the primary widget's resolved one). */
    private HudStyle.Resolved shownStyle(HudElement p) {
        if (global) return HudStyle.resolve(HudLayout.globalStyle(), null, scratch);
        return HudLayout.style(p);
    }

    private final HudStyle.Resolved scratch = new HudStyle.Resolved();

    /** The primary widget's own overrides (null in global scope). */
    private HudStyle own(HudElement p) {
        return global ? null : HudLayout.placement(p).style;
    }

    /** Applies a style edit to the global style or to every selected widget. */
    private void edit(Object undoKey, Consumer<HudStyle> change) {
        s.checkpoint(undoKey);
        if (global) {
            change.accept(HudLayout.globalStyle());
        } else {
            for (HudElement e : s.selection) {
                if (!e.styleable()) continue;
                HudLayout.Placement pl = HudLayout.placement(e);
                change.accept(pl.style);
                pl.custom = true;
            }
        }
        HudLayout.styleChanged();
    }

    /** Row label with the "own value" dot; clicking an overridden label resets it to the global value. */
    private boolean label(Gfx g, Ui ui, Object key, int x, int y, String labelKey, boolean overridden) {
        String l = Lang.tr(labelKey);
        int lw = g.width(l) + 8;
        boolean reset = false;
        if (overridden && !global) {
            reset = ui.clicked(x - 2, y, lw + 2, 12);
            boolean hot = ui.hover(x - 2, y, lw + 2, 12);
            g.rect(x, y + 4, 3, 3, Theme.BRAND);
            g.text(l, x + 6, y + 2, hot ? Theme.BRAND_HOVER : Theme.TEXT);
            ui.tooltip(key, x - 2, y, lw + 2, 12, Lang.tr("core.hud.style.inherit.tip"));
        } else {
            g.text(l, x, y + 2, Theme.MUTED);
        }
        return reset;
    }

    private static final int LABEL_W = 56;

    private void boxPage(Gfx g, Ui ui, HudElement p, int x, int y, int w) {
        HudStyle.Resolved st = shownStyle(p);
        HudStyle own = own(p);
        int cx = x + LABEL_W;
        // background on/off
        if (label(g, ui, "l-bg", x, y, "core.hud.style.background", own != null && own.background != null)) edit(null, o -> o.background = null);
        if (Widgets.toggle(ui, "t-bg", x + w - Widgets.TOGGLE_W, y + 1, st.background, true)) {
            boolean v = !st.background;
            edit(null, o -> o.background = v);
        }
        y += ROW;
        if (label(g, ui, "l-bc", x, y, "core.hud.style.color", own != null && own.bgColor != null)) edit(null, o -> o.bgColor = null);
        int picked = swatchRow(g, ui, cx, y, BG_COLORS, st.bg & 0xFFFFFF, false, false, 0);
        if (picked == -2) openPicker(0);
        else if (picked >= 0) edit(null, o -> o.bgColor = picked);
        y += ROW;
        if (label(g, ui, "l-bo", x, y, "core.hud.style.opacity", own != null && own.bgOpacity != null)) edit(null, o -> o.bgOpacity = null);
        float op = ((st.bg >>> 24) & 0xFF) / 255f;
        int sw = w - LABEL_W - 28;
        float nv = Widgets.slider(ui, "s-bo", cx, y, sw, op);
        if (Math.abs(nv - op) > 0.004f) {
            float v = Math.round(nv * 100) / 100f;
            edit("opacity", o -> o.bgOpacity = v);
        }
        g.textRight(HudEditorScreen.pct(Math.round(op * 100)), x + w, y + 2, Theme.TEXT);
        y += ROW;
        if (label(g, ui, "l-co", x, y, "core.hud.style.corners", own != null && own.corners != null)) edit(null, o -> o.corners = null);
        int c = Widgets.segmented(ui, "g-co", cx, y, w - LABEL_W, cornerLabels(), st.corners.ordinal());
        if (c >= 0) edit(null, o -> o.corners = HudStyle.Corners.values()[c]);
        y += ROW;
        if (label(g, ui, "l-bd", x, y, "core.hud.style.border", own != null && own.border != null)) edit(null, o -> o.border = null);
        if (Widgets.toggle(ui, "t-bd", x + w - Widgets.TOGGLE_W, y + 1, st.border, true)) {
            boolean v = !st.border;
            edit(null, o -> o.border = v);
        }
        y += ROW + 4;
        footer(g, ui, p, x, y, w);
    }

    private void textPage(Gfx g, Ui ui, HudElement p, int x, int y, int w) {
        HudStyle.Resolved st = shownStyle(p);
        HudStyle own = own(p);
        int cx = x + LABEL_W;
        if (label(g, ui, "l-tc", x, y, "core.hud.style.textColor", own != null && (own.textColor != null || own.paint != null))) {
            edit(null, o -> {
                o.textColor = null;
                o.paint = null;
            });
        }
        int cur = st.paint == HudStyle.TextPaint.COLOR ? st.text & 0xFFFFFF : -1;
        int mode = st.paint == HudStyle.TextPaint.ACCENT ? 1 : st.paint == HudStyle.TextPaint.RAINBOW ? 2 : 0;
        int picked = swatchRow(g, ui, cx, y, TEXT_COLORS, cur, true, true, mode);
        if (picked == -2) openPicker(1);
        else if (picked == -3) edit(null, o -> o.paint = HudStyle.TextPaint.ACCENT);
        else if (picked == -4) edit(null, o -> o.paint = HudStyle.TextPaint.RAINBOW);
        else if (picked >= 0) edit(null, o -> {
            o.textColor = picked;
            o.paint = HudStyle.TextPaint.COLOR;
        });
        y += ROW;
        if (label(g, ui, "l-ac", x, y, "core.hud.style.accent", own != null && own.accentColor != null)) edit(null, o -> o.accentColor = null);
        int pa = swatchRow(g, ui, cx, y, ACCENT_COLORS, st.accent & 0xFFFFFF, false, false, 0);
        if (pa == -2) openPicker(2);
        else if (pa >= 0) edit(null, o -> o.accentColor = pa);
        y += ROW;
        if (label(g, ui, "l-sh", x, y, "core.hud.style.shadow", own != null && own.shadow != null)) edit(null, o -> o.shadow = null);
        if (Widgets.toggle(ui, "t-sh", x + w - Widgets.TOGGLE_W, y + 1, st.shadow, true)) {
            boolean v = !st.shadow;
            edit(null, o -> o.shadow = v);
        }
        y += ROW;
        if (label(g, ui, "l-al", x, y, "core.hud.style.align", own != null && own.align != null)) edit(null, o -> o.align = null);
        int al = alignControl(g, ui, cx, y, w - LABEL_W, st.align.ordinal());
        if (al >= 0) edit(null, o -> o.align = HudStyle.Align.values()[al]);
        y += ROW + 4;
        footer(g, ui, p, x, y, w);
    }

    private void sizePage(Gfx g, Ui ui, HudElement p, int x, int y, int w, boolean styled) {
        HudLayout.Placement pl = HudLayout.placement(p);
        int cx = x + LABEL_W;
        g.text(Lang.tr("core.hud.style.size"), x, y + 2, Theme.MUTED);
        float min = p.minScale(), max = p.maxScale();
        float t = (pl.scale - min) / Math.max(0.01f, max - min);
        int sw = w - LABEL_W - 28;
        float nt = Widgets.slider(ui, "s-size", cx, y, sw, t);
        if (Math.abs(nt - t) > 0.002f) {
            s.checkpoint("size");
            float target = min + nt * (max - min);
            for (HudElement e : s.selection) HudLayout.scaleTo(e, target, s.width, s.height);
        }
        g.textRight(HudEditorScreen.pct(Math.round(pl.scale * 100)), x + w, y + 2, Theme.TEXT);
        y += ROW + 4;
        if (styled && (p.hasVariants() || anyVariants())) {
            HudStyle own = HudLayout.placement(p).style;
            if (label(g, ui, "l-va", x, y, "core.hud.style.detail", own.variant != null)) edit(null, o -> o.variant = null);
            int v = Widgets.segmented(ui, "g-va", cx, y, w - LABEL_W, variantLabels(), HudLayout.style(p).variant.ordinal());
            if (v >= 0) edit(null, o -> o.variant = HudStyle.Variant.values()[v]);
            y += ROW;
        }
        // anchor: a 3 × 3 grid of the screen points; click = fix it there, "Auto" = follow the position
        g.text(Lang.tr("core.hud.style.anchor"), x, y + 2, Theme.MUTED);
        ui.tooltip("l-anchor", x, y, LABEL_W - 4, 12, Lang.tr("core.hud.style.anchor.tip"));
        int gw = 36, gh = 26;
        g.rect(cx, y, gw, gh, Theme.SURFACE2);
        g.outline(cx, y, gw, gh, Theme.LINE2);
        for (Anchor a : Anchor.values()) {
            int ax = cx + 3 + a.col * 13, ay = y + 3 + a.row * 9;
            boolean on = pl.anchor == a;
            boolean hot = ui.hover(ax - 2, ay - 2, 9, 7);
            if (ui.clicked(ax - 2, ay - 2, 9, 7)) {
                s.checkpoint(null);
                for (HudElement e : s.selection) HudLayout.setAnchor(e, a, s.width, s.height);
            }
            int col = on ? (pl.autoAnchor ? Theme.BRAND2 : Theme.BRAND) : hot ? Theme.TEXT : 0x50FFFFFF;
            g.rect(ax, ay, 5, 3, col);
        }
        String auto = Lang.tr("core.hud.style.anchor.auto");
        int bw = g.width(auto) + 14;
        if (Widgets.button(ui, "b-auto", cx + gw + 6, y + 5, bw, 16, auto, pl.autoAnchor ? Widgets.Style.PRIMARY : Widgets.Style.SECONDARY)) {
            s.checkpoint(null);
            for (HudElement e : s.selection) HudLayout.setAnchor(e, null, s.width, s.height);
        }
        String where = Lang.tr(ANCHOR_KEYS[pl.anchor.ordinal()]);
        g.text(g.clip(where, w - LABEL_W - gw - bw - 10), cx + gw + bw + 10, y + 9, Theme.DIM);
        y += 32;
        if (!p.options().isEmpty() && s.selection.size() == 1) {
            Widgets.caption(g, Lang.tr("core.hud.style.options"), x, y + 2);
            y += 14;
            for (HudOption o : p.options()) {
                y = option(g, ui, p, o, x, y, w);
            }
        }
    }

    private static final String[] ANCHOR_KEYS = new String[Anchor.values().length];

    static {
        for (Anchor a : Anchor.values()) ANCHOR_KEYS[a.ordinal()] = "core.hud.anchor." + a.name().toLowerCase(java.util.Locale.ROOT);
    }

    private int option(Gfx g, Ui ui, HudElement p, HudOption o, int x, int y, int w) {
        HudLayout.Placement pl = HudLayout.placement(p);
        String v = pl.opts.getOrDefault(o.key, o.def);
        g.text(g.clip(Lang.tr(o.labelKey), 88), x, y + 2, Theme.MUTED);
        if (o.kind == HudOption.Kind.TOGGLE) {
            if (Widgets.toggle(ui, o, x + w - Widgets.TOGGLE_W, y + 1, "1".equals(v), true)) {
                s.checkpoint(null);
                HudLayout.setOption(p, o.key, "1".equals(v) ? "0" : "1");
            }
        } else {
            int capW = o.captureLabel() != null ? 18 : 0;
            int cw = w - 92 - capW - (capW > 0 ? 2 : 0);
            int dir = Widgets.cycler(ui, o, x + 92, y, cw, o.displayValue(v));
            if (dir != 0) {
                int i = o.values.indexOf(v);
                int n = o.values.size();
                int ni = i < 0 ? 0 : Math.floorMod(i + dir, n);
                s.checkpoint(null);
                HudLayout.setOption(p, o.key, o.values.get(ni));
            }
            if (capW > 0) {
                int bx = x + w - 18;
                if (s.iconButton(g, key(o, 0), bx, y - 1, HudIcons.PASTE, true)) {
                    String c = o.captureValue();
                    if (c != null) {
                        s.checkpoint(null);
                        HudLayout.setOption(p, o.key, c);
                    }
                }
                ui.tooltip(key(o, 0), bx, y - 1, 18, 18, Lang.tr(o.captureLabel()));
            }
        }
        return y + ROW;
    }

    private void footer(Gfx g, Ui ui, HudElement p, int x, int y, int w) {
        if (global) {
            for (String line : wrap(g, Lang.tr("core.hud.style.globalNote"), w)) {
                g.text(line, x, y, Theme.DIM);
                y += 10;
            }
            return;
        }
        boolean any = false;
        for (HudElement e : s.selection) any |= e.styleable() && !HudLayout.placement(e).style.isEmpty();
        String l = Lang.tr("core.hud.style.useGlobal");
        int bw = g.width(l) + 14;
        if (Widgets.button(ui, "b-useglobal", x, y, bw, 16, l, any ? Widgets.Style.SECONDARY : Widgets.Style.DISABLED)) {
            edit(null, HudStyle::clear);
        }
        String go = Lang.tr("core.hud.style.editGlobal");
        int gw = g.width(go) + 10;
        if (Widgets.button(ui, "b-editglobal", x + w - gw, y, gw, 16, go, Widgets.Style.GHOST)) {
            global = true;
            picker = -1;
        }
    }

    private String[] cornerCache, variantCache;
    private String cornerLang;

    private String[] cornerLabels() {
        refreshLabelCache();
        return cornerCache;
    }

    private String[] variantLabels() {
        refreshLabelCache();
        return variantCache;
    }

    private void refreshLabelCache() {
        String lang = Lang.tr("core.hud.style.corners.square");
        if (cornerCache == null || cornerLang != lang) {
            cornerLang = lang;
            cornerCache = new String[] {lang, Lang.tr("core.hud.style.corners.bevel")};
            variantCache = new String[] {Lang.tr("core.hud.style.detail.compact"), Lang.tr("core.hud.style.detail.detailed")};
        }
    }

    /**
     * Swatches + (optional accent "A" and rainbow) + a custom-colour swatch. Returns the picked RGB, -2 = custom,
     * -3 = accent, -4 = rainbow, -1 = nothing.
     */
    private int swatchRow(Gfx g, Ui ui, int x, int y, int[] colors, int current, boolean accent, boolean rainbow, int mode) {
        int s = 10, gap = 2;
        int result = -1;
        boolean known = false;
        for (int c : colors) {
            boolean sel = current == c;
            known |= sel;
            if (swatch(g, ui, x, y + 1, s, c | 0xFF000000, sel)) result = c;
            x += s + gap;
        }
        if (accent) {
            boolean sel = mode == 1;
            if (swatch(g, ui, x, y + 1, s, 0xFF16161C, sel)) result = -3;
            g.text("A", x + 3, y + 2, Theme.BRAND_HOVER);
            ui.tooltip("sw-accent", x, y + 1, s, s, Lang.tr("core.hud.style.accentSwatch"));
            x += s + gap;
        }
        if (rainbow) {
            boolean sel = mode == 2;
            if (swatch(g, ui, x, y + 1, s, 0xFF000000, sel)) result = -4;
            long now = System.currentTimeMillis();
            for (int i = 0; i < s; i++) g.rect(x + i, y + 1, 1, s, HudPaint.rainbowColor((now % 3000L) / 3000f + i / 10f, 0xFF));
            ui.tooltip("sw-rainbow", x, y + 1, s, s, Lang.tr("core.hud.style.rainbow"));
            x += s + gap;
        }
        // custom: shows the current colour if it is not one of the swatches
        boolean customSel = !known && mode == 0 && current >= 0;
        if (swatch(g, ui, x, y + 1, s, customSel ? 0xFF000000 | current : 0xFF16161C, customSel)) result = -2;
        if (!customSel) {
            g.rect(x + 4, y + 3, 2, 6, Theme.MUTED);
            g.rect(x + 2, y + 5, 6, 2, Theme.MUTED);
        }
        ui.tooltip(accent ? "sw-custom-t" : "sw-custom-b", x, y + 1, s, s, Lang.tr("core.hud.style.custom"));
        return result;
    }

    private boolean swatch(Gfx g, Ui ui, int x, int y, int s, int argb, boolean sel) {
        boolean clicked = ui.clicked(x - 1, y - 1, s + 2, s + 2);
        boolean hov = ui.hover(x - 1, y - 1, s + 2, s + 2);
        g.rect(x, y, s, s, argb);
        if ((argb & 0xFFFFFF) < 0x202020) g.outline(x, y, s, s, Theme.LINE2);
        if (sel) g.outline(x - 1, y - 1, s + 2, s + 2, 0xFFFFFFFF);
        else if (hov) g.outline(x - 1, y - 1, s + 2, s + 2, Theme.MUTED);
        return clicked;
    }

    private static final String[] ALIGN_KEYS = {"auto", "left", "center", "right"};
    private static final String[] ALIGN_LABELS = {"core.hud.style.align.auto", "core.hud.style.align.left",
            "core.hud.style.align.center", "core.hud.style.align.right"};

    /** Auto + three alignment icons. */
    private int alignControl(Gfx g, Ui ui, int x, int y, int w, int selected) {
        int h = 16;
        int result = -1;
        g.rect(x, y, w, h, Theme.SURFACE);
        g.outline(x, y, w, h, Theme.LINE2);
        String auto = Lang.tr("core.hud.style.align.auto");
        int aw = Math.max(28, g.width(auto) + 10);
        int segW = (w - aw) / 3;
        int sx = x;
        for (int i = 0; i < 4; i++) {
            int sw = i == 0 ? aw : i == 3 ? x + w - sx : segW;
            boolean sel = i == selected;
            if (ui.clicked(sx, y, sw, h)) result = i;
            boolean hot = ui.hover(sx, y, sw, h);
            ui.tooltip(ALIGN_KEYS[i], sx, y, sw, h, Lang.tr(ALIGN_LABELS[i]));
            if (sel) g.rect(sx + 1, y + 1, sw - 2, h - 2, Theme.BRAND);
            else if (hot) g.rect(sx + 1, y + 1, sw - 2, h - 2, Theme.SURFACE3);
            int c = sel ? 0xFFFFFFFF : hot ? Theme.TEXT : Theme.MUTED;
            if (i == 0) g.textCenter(auto, sx + sw / 2, y + 4, c);
            else {
                int ix = sx + sw / 2 - 4, iy = y + 4;
                for (int l = 0; l < 4; l++) {
                    int lw = l % 2 == 0 ? 9 : 5;
                    int lx = i == 1 ? ix : i == 2 ? ix + (9 - lw) / 2 : ix + 9 - lw;
                    g.rect(lx, iy + l * 2, lw, 1, c);
                }
            }
            sx += sw;
        }
        return result;
    }

    // ============================================================================================ colour picker

    private void openPicker(int which) {
        picker = which;
        HudElement p = s.primary();
        HudStyle.Resolved st = p == null ? null : shownStyle(p);
        int rgb = st == null ? 0xFFFFFF : which == 0 ? st.bg : which == 1 ? st.text : st.accent;
        rgbToHsv(rgb & 0xFFFFFF, hsv);
        hexEditing = false;
    }

    private void picker(Gfx g, Ui ui, int x, int y, int w) {
        String title = Lang.tr(picker == 0 ? "core.hud.style.background" : picker == 1 ? "core.hud.style.textColor" : "core.hud.style.accent");
        g.text(title, x, y, Theme.MUTED);
        y += 12;
        int pw = w, ph = 50;
        int cell = 4;
        int cols = pw / cell, rows = ph / cell;
        pw = cols * cell;
        ph = rows * cell;
        for (int cx = 0; cx < cols; cx++) {
            float sat = cx / (float) (cols - 1);
            for (int cy = 0; cy < rows; cy++) {
                float val = 1f - cy / (float) (rows - 1);
                g.rect(x + cx * cell, y + cy * cell, cell, cell, 0xFF000000 | HudPaint.hsv(hsv[0], sat, val));
            }
        }
        g.outline(x - 1, y - 1, pw + 2, ph + 2, Theme.LINE2);
        if (ui.dragging("pick-sv", x, y, pw, ph)) {
            hsv[1] = clamp01((ui.mouseX - x) / (float) (pw - 1));
            hsv[2] = 1f - clamp01((ui.mouseY - y) / (float) (ph - 1));
            applyPicked();
        }
        int kx = x + Math.round(hsv[1] * (pw - 1)), ky = y + Math.round((1f - hsv[2]) * (ph - 1));
        g.outline(kx - 2, ky - 2, 5, 5, 0xFFFFFFFF);
        g.outline(kx - 3, ky - 3, 7, 7, 0xFF000000);
        y += ph + 5;
        int hh = 7;
        for (int i = 0; i < pw; i += 2) g.rect(x + i, y, 2, hh, 0xFF000000 | HudPaint.hsv(i / (float) pw, 1f, 1f));
        if (ui.dragging("pick-h", x, y - 1, pw, hh + 2)) {
            hsv[0] = clamp01((ui.mouseX - x) / (float) pw) * 0.999f;
            applyPicked();
        }
        int hx = x + Math.round(hsv[0] * pw);
        g.rect(hx - 1, y - 1, 3, hh + 2, 0xFFFFFFFF);
        y += hh + 6;
        int rgb = HudPaint.hsv(hsv[0], hsv[1], hsv[2]);
        g.rect(x, y, 16, 16, 0xFF000000 | rgb);
        g.outline(x, y, 16, 16, Theme.LINE2);
        if (rgb != hexRgb || hexText == null) {
            hexRgb = rgb;
            hexText = "#" + HudStyle.hex(rgb).toUpperCase(java.util.Locale.ROOT);
        }
        EditBox hex = Widgets.textField(ui, "pick-hex", x + 20, y, 62, hexText, 7, v -> {
            String t = v.startsWith("#") ? v.substring(1) : v;
            if (t.matches("[0-9a-fA-F]{6}")) {
                rgbToHsv(Integer.parseInt(t, 16), hsv);
                applyPicked();
            }
        });
        hexEditing = hex.isFocused();
        String done = Lang.tr("core.hud.style.picker.done");
        int bw = g.width(done) + 16;
        if (Widgets.button(ui, "pick-done", x + w - bw, y, bw, 16, done, Widgets.Style.PRIMARY)) {
            picker = -1;
            ui.blurInputs();
        }
    }

    private int hexRgb = -1;
    private String hexText;

    private void applyPicked() {
        int rgb = HudPaint.hsv(hsv[0], hsv[1], hsv[2]);
        int which = picker;
        edit("picker" + which, o -> {
            if (which == 0) o.bgColor = rgb;
            else if (which == 1) {
                o.textColor = rgb;
                o.paint = HudStyle.TextPaint.COLOR;
            } else o.accentColor = rgb;
        });
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    static void rgbToHsv(int rgb, float[] out) {
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h;
        if (d == 0) h = 0;
        else if (max == r) h = ((g - b) / d) % 6f;
        else if (max == g) h = (b - r) / d + 2f;
        else h = (r - g) / d + 4f;
        h /= 6f;
        if (h < 0) h += 1f;
        out[0] = h;
        out[1] = max == 0 ? 0 : d / max;
        out[2] = max;
    }

    // ============================================================================================ toast

    private void toastDraw(Gfx g, long now) {
        if (toast == null) return;
        long age = now - toastAt;
        if (age > 2600) {
            toast = null;
            return;
        }
        float a = age < 150 ? age / 150f : age > 2300 ? (2600 - age) / 300f : 1f;
        int w = g.width(toast) + 22;
        int x = (s.width - w) / 2;
        boolean dockTop = s.dockY < s.height / 2;
        int y = dockTop ? s.dockY + s.dockH + 18 : s.dockY - 34;
        y += Math.round((1f - a) * 4);
        g.rect(x, y, w, 16, Theme.alpha(0xF0121217, a));
        g.outline(x, y, w, 16, Theme.alpha(Theme.BRAND, 0.7f * a));
        Icons.draw(g, Icons.CHECK, x + 5, y + 4, 1, Theme.alpha(Theme.BRAND2, a));
        g.text(toast, x + 17, y + 4, Theme.alpha(Theme.TEXT, a));
    }

    // ============================================================================================ intro

    private void intro(Gfx g, Ui ui, long now) {
        float a = introAnim.to(1f);
        g.rect(0, 0, s.width, s.height, Theme.alpha(0xA0000000, a));
        int w = Math.min(250, s.width - 20), h = 150;
        int x = (s.width - w) / 2, y = (s.height - h) / 2 + Math.round((1f - a) * 6);
        g.rect(x + 3, y + 3, w, h, 0x70000000);
        g.rect(x, y, w, h, 0xFF0B0B0E);
        g.outline(x, y, w, h, Theme.LINE2);
        g.rect(x, y, w, 1, Theme.BRAND);
        // illustration
        int ix = x + 10, iy = y + 10, iw = w - 20, ih = 56;
        g.rect(ix, iy, iw, ih, 0xFF121217);
        g.outline(ix, iy, iw, ih, Theme.LINE);
        float t = ((now - introStepAt) % 2400L) / 2400f;
        illustration(g, intro, ix, iy, iw, ih, t);
        int ty = iy + ih + 10;
        String step = trc("core.hud.intro.step", intro + 1);
        g.text(step, x + 10, ty, Theme.BRAND_HOVER);
        g.bold(Lang.tr(INTRO_KEYS[intro * 2]), x + 10 + g.width(step) + 6, ty, Theme.TEXT);
        ty += 13;
        for (String line : wrap(g, Lang.tr(INTRO_KEYS[intro * 2 + 1]), w - 20)) {
            g.text(line, x + 10, ty, Theme.MUTED);
            ty += 10;
        }
        int by = y + h - 24;
        for (int i = 0; i < 3; i++) g.rect(x + 10 + i * 9, by + 6, i == intro ? 6 : 4, 4, i == intro ? Theme.BRAND : Theme.SURFACE3);
        String next = Lang.tr(intro >= 2 ? "core.hud.intro.start" : "core.hud.intro.next");
        String skip = Lang.tr("core.hud.intro.skip");
        int nw = g.width(next) + 18, kw = g.width(skip) + 14;
        if (Widgets.button(ui, "intro-next", x + w - 10 - nw, by, nw, 16, next, Widgets.Style.PRIMARY)) nextIntro();
        if (intro < 2 && Widgets.button(ui, "intro-skip", x + w - 14 - nw - kw, by, kw, 16, skip, Widgets.Style.GHOST)) closeIntro();
        ui.clickedNoFocus(0, 0, s.width, s.height);
    }

    private static final String[] INTRO_KEYS = {"core.hud.intro.1.title", "core.hud.intro.1.body",
            "core.hud.intro.2.title", "core.hud.intro.2.body", "core.hud.intro.3.title", "core.hud.intro.3.body"};

    /** Small animated drawings: drag, resize, right-click menu. */
    private void illustration(Gfx g, int step, int x, int y, int w, int h, float t) {
        float e = Anim.ease(t < 0.15f ? 0 : t > 0.75f ? 1 : (t - 0.15f) / 0.6f);
        int cx = x + w / 2;
        switch (step) {
            case 0 -> {
                int bx = Math.round(x + 18 + (w - 90) * e), by = y + 16;
                widgetSketch(g, bx, by, 44, 14, true);
                // guide when it reaches the middle
                if (Math.abs(bx + 22 - cx) < 3) g.rect(cx, y + 2, 1, h - 4, Theme.BRAND2);
                cursorSketch(g, bx + 30, by + 9, true);
            }
            case 1 -> {
                int bw = Math.round(40 + 40 * e), bh = Math.round(12 + 10 * e);
                int bx = cx - 40, by = y + 12;
                widgetSketch(g, bx, by, bw, bh, true);
                g.rect(bx + bw - 3, by + bh - 3, 5, 5, Theme.BRAND);
                g.rect(bx + bw - 2, by + bh - 2, 3, 3, 0xFFFFFFFF);
                cursorSketch(g, bx + bw + 1, by + bh + 1, true);
            }
            default -> {
                int bx = cx - 60, by = y + 18;
                widgetSketch(g, bx, by, 44, 14, true);
                cursorSketch(g, bx + 30, by + 9, false);
                if (t > 0.3f) {
                    float m = Math.min(1f, (t - 0.3f) / 0.15f);
                    int mx = bx + 34, my = by + 12 + Math.round((1 - m) * 3);
                    g.rect(mx, my, 62, 34, Theme.alpha(0xFF0B0B0E, m));
                    g.outline(mx, my, 62, 34, Theme.alpha(Theme.LINE2, m));
                    g.rect(mx + 1, my + 2, 60, 10, Theme.alpha(Theme.BRAND, 0.3f * m));
                    g.text(Lang.tr("core.hud.menu.style"), mx + 5, my + 3, Theme.alpha(Theme.TEXT, m));
                    g.rect(mx + 5, my + 17, 36, 2, Theme.alpha(Theme.MUTED, 0.6f * m));
                    g.rect(mx + 5, my + 25, 28, 2, Theme.alpha(Theme.MUTED, 0.6f * m));
                }
            }
        }
    }

    private static void widgetSketch(Gfx g, int x, int y, int w, int h, boolean selected) {
        g.rect(x, y, w, h, 0xA00B0B0E);
        g.rect(x + 4, y + h / 2 - 1, Math.min(12, w / 3), 3, Theme.BRAND);
        g.rect(x + 18, y + h / 2 - 1, Math.max(4, w - 24), 3, Theme.TEXT);
        g.outline(x - 1, y - 1, w + 2, h + 2, selected ? Theme.BRAND : Theme.LINE2);
    }

    private static void cursorSketch(Gfx g, int x, int y, boolean left) {
        // a tiny mouse pointer
        for (int i = 0; i < 7; i++) g.rect(x, y + i, Math.max(1, i < 5 ? i + 1 : 7 - i), 1, 0xFFFFFFFF);
        g.rect(x + 2, y + 6, 1, 3, 0xFFFFFFFF);
        if (!left) {
            g.rect(x + 8, y - 4, 6, 8, Theme.SURFACE3);
            g.rect(x + 11, y - 4, 3, 3, Theme.BRAND2);
        }
    }

}
