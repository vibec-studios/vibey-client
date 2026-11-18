package dev.henny.vibeyclient.ui;

import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.ClientCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Vibey menu: a square window with the wordmark, the tab bar ({@link VibeyTabs}) and the panel of the current
 * tab. Opens with Right Shift (keybind "Open Vibey menu") and from the pause menu.
 */
public class VibeyScreen extends BaseScreen {
    public static final int HEADER = 30;

    private final Screen parent;
    private final Ui ui = new Ui(this::addWidget);
    private final Map<String, Panel> panels = new HashMap<>();
    private final Anim open = new Anim(0f);
    private String tab;
    private Panel current;

    public VibeyScreen(Screen parent) {
        this(parent, VibeyTabs.MODULES);
    }

    public VibeyScreen(Screen parent, String tab) {
        super(Component.literal("Vibey"));
        this.parent = parent;
        this.tab = tab;
    }

    @Override
    protected void init() {
        ui.reattach();
    }

    /** Switches to another tab (no-op if it does not exist). */
    public void showTab(String id) {
        for (VibeyTabs.Tab t : VibeyTabs.visible()) {
            if (t.id().equals(id)) {
                if (current != null) current.onHide();
                tab = id;
                current = panels.computeIfAbsent(id, k -> t.factory().get());
                current.onShow();
                return;
            }
        }
    }

    public String tab() {
        return tab;
    }

    // ============================================================================================ layout

    public int windowWidth() {
        return Math.min(width - 24, 640);
    }

    public int windowHeight() {
        return Math.min(height - 24, 380);
    }

    @Override
    protected void draw(Gfx g, int mx, int my, float delta) {
        if (current == null) showTab(tab);
        if (current == null) showTab(VibeyTabs.MODULES);
        ui.begin(g, mx, my, delta);
        float o = open.to(1f);

        boolean world = Minecraft.getInstance().level != null;
        g.rect(0, 0, width, height, Theme.alpha(world ? Theme.SCRIM : Theme.BG, world ? o : 1f));

        int w = windowWidth();
        int h = windowHeight();
        int x = (width - w) / 2;
        int y = (height - h) / 2 + Math.round((1f - o) * 8);

        g.rect(x, y, w, h, Theme.SURFACE);
        g.outline(x - 1, y - 1, w + 2, h + 2, Theme.LINE2);
        drawHeader(g, x, y, w);
        g.hline(x, y + HEADER, w, Theme.LINE);

        if (current != null) current.draw(ui, x, y + HEADER + 1, w, h - HEADER - 1);

        // Fade the whole window in: cheap overlay instead of per-widget alpha.
        if (o < 1f) g.rect(x - 1, y - 1, w + 2, h + 2, Theme.alpha(Theme.SURFACE, 1f - o));
        ui.end();
    }

    private void drawHeader(Gfx g, int x, int y, int w) {
        int lx = x + Theme.PAD;
        g.image(Compat.id("icon.png"), lx, y + 7, 16, 16, 512);
        g.textScaled("VIBEY", lx + 22, y + 8, 2, Theme.TEXT, true);
        int wx = lx + 22 + g.boldWidth("VIBEY") * 2 + 6;
        g.text("CLIENT", wx, y + 15, Theme.BRAND);
        g.text(BuildInfo.VERSION, wx + g.width("CLIENT") + 6, y + 15, Theme.DIM);

        // close
        int cx = x + w - 24;
        boolean ch = ui.hover(cx, y + 6, 18, 18);
        if (ui.clicked(cx, y + 6, 18, 18)) onClose();
        if (ch) g.rect(cx, y + 6, 18, 18, Theme.SURFACE3);
        Widgets.cross(g, cx + 5, y + 11, 8, ch ? Theme.TEXT : Theme.MUTED);

        // tabs, right-aligned before the close button
        List<VibeyTabs.Tab> tabs = VibeyTabs.visible();
        int tx = cx - 8;
        for (int i = tabs.size() - 1; i >= 0; i--) {
            VibeyTabs.Tab t = tabs.get(i);
            String label = Lang.tr(t.labelKey());
            int tw = g.width(label) + 16;
            tx -= tw;
            boolean sel = t.id().equals(tab);
            if (ui.clicked(tx, y, tw, HEADER)) showTab(t.id());
            float hov = ui.anim(t, 0f).to(ui.hover(tx, y, tw, HEADER) || sel ? 1f : 0f);
            g.text(label, tx + 8, y + 11, Theme.mix(Theme.MUTED, Theme.TEXT, hov));
            float s = ui.anim(t, 5, sel ? 1f : 0f).to(sel ? 1f : 0f);
            if (s > 0f) g.rect(tx + 8, y + HEADER - 2, Math.round((tw - 16) * s), 2, Theme.BRAND);
        }
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
        if (current != null && current.key(keyCode)) return true;
        // Right Shift (or whatever opens the menu) closes it again, unless typing.
        if (!ui.anyInputFocused() && ClientCompat.matches(dev.henny.vibeyclient.api.GlobalSettings.openMenu.mapping(), keyCode)) {
            onClose();
            return true;
        }
        return false;
    }

    /** Owner of a keybind capture ({@link Ui#capture}): gets the next key code (Escape = cancel, -1 = unbind). */
    public interface KeyCapture {
        void accept(int keyCode);
    }

    @Override
    public void onClose() {
        if (current != null) current.onHide();
        ClientCompat.openScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return parent != null && parent.isPauseScreen();
    }
}
