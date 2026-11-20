package dev.henny.vibeyclient.ui;

import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Vibey menu: a square window with the seal + wordmark, the tab bar ({@link VibeyTabs}) and the panel of the
 * current tab. Opens with Right Shift (keybind "Open Vibey menu"), from the pause menu and the title screen.
 * Keys: Tab / Shift+Tab move the focus, Enter clicks, Ctrl+Tab switches tabs, Esc cancels a key capture, leaves a
 * text field, then closes.
 */
public class VibeyScreen extends BaseScreen {
    public static final int HEADER = 30;

    private final Screen parent;
    private final Ui ui = new Ui(this::addWidget);
    private final Map<String, Panel> panels = new HashMap<>();
    private final Anim open = new Anim(0f);
    private final Anim content = new Anim(1f);
    private final Anim barX = new Anim(-1f);
    private final Anim barW = new Anim(0f);
    private final Seal seal = new Seal(false);
    private String tab;
    private Panel current;
    private int tabDir = 1;

    public VibeyScreen(Screen parent) {
        this(parent, VibeyTabs.MODULES);
    }

    public VibeyScreen(Screen parent, String tab) {
        super(Component.literal("Vibey"));
        this.parent = parent;
        this.tab = tab;
    }

    public boolean typing() {
        return ui.anyInputFocused();
    }

    @Override
    protected void init() {
        UiScale.apply(this);
        ui.reattach();
    }

    /** Switches to another tab (no-op if it does not exist). */
    public void showTab(String id) {
        List<VibeyTabs.Tab> tabs = VibeyTabs.visible();
        int oldIndex = -1, newIndex = -1;
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).id().equals(tab)) oldIndex = i;
            if (tabs.get(i).id().equals(id)) newIndex = i;
        }
        if (newIndex < 0) return;
        VibeyTabs.Tab t = tabs.get(newIndex);
        if (current != null && id.equals(tab)) return;
        if (current != null) current.onHide();
        tabDir = newIndex >= oldIndex ? 1 : -1;
        tab = id;
        current = panels.computeIfAbsent(id, k -> t.factory().get());
        current.onShow();
        content.snap(0f);
        ui.clearFocus();
    }

    public String tab() {
        return tab;
    }

    /** The screen the menu returns to. */
    public Screen parent() {
        return parent;
    }

    // ============================================================================================ layout

    public int windowWidth() {
        return Math.min(width - 24, 680);
    }

    public int windowHeight() {
        return Math.min(height - 24, 400);
    }

    @Override
    protected void draw(Gfx g, int mx, int my, float delta) {
        if (current == null) showTab(tab);
        if (current == null) showTab(VibeyTabs.MODULES);
        ui.begin(g, mx, my, delta);
        float o = open.to(1f);

        boolean world = Minecraft.getInstance().level != null;
        if (world) {
            g.rect(0, 0, width, height, Theme.alpha(Theme.SCRIM, o));
        } else {
            panorama(g, delta);
            g.rect(0, 0, width, height, 0xD8000000);
        }

        int w = windowWidth();
        int h = windowHeight();
        int x = (width - w) / 2;
        int y = (height - h) / 2 + Math.round((1f - o) * 8);

        g.rect(x, y, w, h, Theme.SURFACE);
        g.outline(x - 1, y - 1, w + 2, h + 2, Theme.LINE2);
        g.rect(x, y, w, HEADER, Theme.PANEL);
        drawHeader(g, x, y, w);
        g.hline(x, y + HEADER, w, Theme.LINE);

        float c = content.to(1f);
        int slide = Math.round((1f - c) * 6 * tabDir);
        if (current != null) current.draw(ui, x + slide, y + HEADER + 1, w, h - HEADER - 1);
        if (c < 1f) {
            g.layer();
            g.rect(x, y + HEADER + 1, w, h - HEADER - 1, Theme.alpha(Theme.SURFACE, (1f - c) * 0.9f));
        }

        // Fade the whole window in: cheap overlay instead of per-widget alpha.
        if (o < 1f) {
            g.layer();
            g.rect(x - 1, y - 1, w + 2, h + 2, Theme.alpha(Theme.SURFACE, 1f - o));
        }
        ui.end();
    }

    private void drawHeader(Gfx g, int x, int y, int w) {
        int lx = x + Theme.PAD;
        float unit = 0.75f;
        int sh = seal.height(unit);
        boolean sealHover = ui.hover(lx, y + 2, seal.width(unit), HEADER - 4);
        if (ui.clickedNoFocus(lx, y + 2, seal.width(unit), HEADER - 4)) seal.cheer();
        int sw = seal.draw(g, lx, y + (HEADER - sh) / 2, unit, sealHover);
        int wx = lx + sw + 6;
        PixelFont.drawExtruded(g, "VIBEY", wx, y + 10, 2, Theme.TEXT, Theme.BRAND);
        int brandRight = wx + PixelFont.width("VIBEY", 2) + 1;

        // close
        int cx = x + w - 26;
        boolean cf = ui.clicked(cx, y + 5, 20, 20);
        boolean ch = ui.focusedLast() || ui.hover(cx, y + 5, 20, 20);
        if (ch) g.rect(cx, y + 5, 20, 20, Theme.SURFACE3);
        Icons.draw(g, Icons.CLOSE, cx + 6, y + 11, 1, ch ? Theme.TEXT : Theme.MUTED);
        ui.tooltip("close", cx, y + 5, 20, 20, Lang.tr("core.menu.close"));

        // tabs, left-aligned after the brand; icons only when the labels do not fit
        List<VibeyTabs.Tab> tabs = VibeyTabs.visible();
        int tx = brandRight + 18;
        int room = cx - 8 - tx;
        int labelsW = 0;
        for (VibeyTabs.Tab t : tabs) labelsW += tabWidth(g, t, false);
        boolean compact = labelsW > room;
        int selX = -1, selW = 0;
        for (VibeyTabs.Tab t : tabs) {
            int tw = tabWidth(g, t, compact);
            boolean sel = t.id().equals(tab);
            boolean clicked = ui.clicked(tx, y, tw, HEADER);
            boolean hot = ui.focusedLast() || ui.hover(tx, y, tw, HEADER);
            float hov = ui.anim(t, 0f).to(hot || sel ? 1f : 0f);
            int color = Theme.mix(Theme.MUTED, Theme.TEXT, hov);
            String label = Lang.tr(t.labelKey());
            boolean icon = Icons.has(t.icon());
            if (compact && icon) {
                Icons.draw(g, t.icon(), tx + (tw - Icons.SIZE) / 2, y + 11, sel ? Theme.BRAND : color);
                ui.tooltip(t, tx, y, tw, HEADER, label);
            } else {
                int lx2 = tx + 8;
                if (icon && !compact) {
                    Icons.draw(g, t.icon(), lx2, y + 11, sel ? Theme.BRAND : Theme.mix(Theme.DIM, Theme.MUTED, hov));
                    lx2 += Icons.SIZE + 5;
                }
                g.text(g.clip(label, tx + tw - lx2 - 4), lx2, y + 11, color);
            }
            if (sel) {
                selX = tx + 4;
                selW = tw - 8;
            }
            if (clicked) showTab(t.id());
            tx += tw;
        }
        if (selX >= 0) {
            if (barX.target() < 0) {
                barX.snap(selX);
                barW.snap(selW);
            }
            int bx = Math.round(barX.to(selX));
            int bw = Math.round(barW.to(selW));
            g.rect(bx, y + HEADER - 2, bw, 2, Theme.BRAND);
        }
    }

    private static int tabWidth(Gfx g, VibeyTabs.Tab t, boolean compact) {
        boolean icon = Icons.has(t.icon());
        if (compact && icon) return 26;
        return g.width(Lang.tr(t.labelKey())) + 16 + (icon ? Icons.SIZE + 5 : 0);
    }

    // ============================================================================================ input

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
        Object cap = ui.capturing();
        if (cap instanceof KeyCapture kc) {
            ui.stopCapture();
            kc.accept(keyCode);
            return true;
        }
        if (keyCode == Keys.ESCAPE && ui.anyInputFocused()) {
            ui.blurInputs();
            return true;
        }
        if (current != null && current.key(keyCode)) return true;
        if (keyCode == Keys.TAB && ClientCompat.controlDown()) {
            cycleTab(ClientCompat.shiftDown() ? -1 : 1);
            return true;
        }
        if (ui.anyInputFocused()) {
            if (keyCode == Keys.ESCAPE || keyCode == Keys.TAB) {
                ui.blurInputs();
                if (keyCode == Keys.TAB) ui.key(keyCode, ClientCompat.shiftDown(), false);
                return true;
            }
            return false;
        }
        if (ui.key(keyCode, ClientCompat.shiftDown(), false)) return true;
        // Right Shift (or whatever opens the menu) closes it again, unless typing.
        if (ClientCompat.matches(GlobalSettings.openMenu.mapping(), keyCode)) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    protected boolean typed(int codepoint) {
        if (ui.anyInputFocused() || ui.capturing() != null) return false;
        return current != null && current.typed(codepoint);
    }

    private void cycleTab(int dir) {
        List<VibeyTabs.Tab> tabs = VibeyTabs.visible();
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).id().equals(tab)) {
                showTab(tabs.get(Math.floorMod(i + dir, tabs.size())).id());
                return;
            }
        }
    }

    /** Owner of a keybind capture ({@link Ui#capture}): gets the next key code (Escape = cancel, -1 = unbind). */
    public interface KeyCapture {
        void accept(int keyCode);
    }

    @Override
    public void onClose() {
        if (current != null) current.onHide();
        current = null;
        ClientCompat.openScreen(parent);
    }

    @Override
    public void removed() {
        seal.close();
        UiScale.restore();
    }

    @Override
    public boolean isPauseScreen() {
        return parent != null && parent.isPauseScreen();
    }

    /** Version line used in the menu ("Vibey Client 0.1.0 · Fabric · Minecraft 1.21.11"). */
    public static String versionLine() {
        return "Vibey Client " + BuildInfo.VERSION + " · " + dev.henny.vibeyclient.Vibey.platform().loader() + " · Minecraft " + BuildInfo.MINECRAFT_LABEL;
    }
}
