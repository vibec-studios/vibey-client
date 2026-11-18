package dev.henny.vibeyclient.module.social.ui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.social.SocialCompat;
import dev.henny.vibeyclient.ui.Anim;
import dev.henny.vibeyclient.ui.BaseScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.UiHost;
import dev.henny.vibeyclient.ui.Widgets;
import dev.henny.vibeyclient.ui.toast.TextWrap;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

/** A small Vibe-styled yes/no dialog over the current screen (or the world). */
public final class ConfirmScreen extends BaseScreen {
    private final Screen parent;
    private final String heading;
    private final String text;
    private final String confirm;
    private final Runnable onYes;
    private final Ui ui = UiHost.create(this::addWidget);
    private final Anim open = new Anim(0f);
    private List<String> lines;
    private int linesW = -1;

    private ConfirmScreen(Screen parent, String heading, String text, String confirm, Runnable onYes) {
        super(SocialCompat.literal(heading));
        this.parent = parent;
        this.heading = heading;
        this.text = text;
        this.confirm = confirm;
        this.onYes = onYes;
    }

    /** Asks over whatever is open; {@code onYes} runs on the client thread after the dialog closed. */
    public static void ask(String heading, String text, String confirmLabel, Runnable onYes) {
        Screen parent = ClientCompat.screen();
        if (parent instanceof ConfirmScreen c) parent = c.parent;
        ClientCompat.openScreen(new ConfirmScreen(parent, heading, text, confirmLabel, onYes));
    }

    @Override
    protected void init() {
        if (parent != null) SocialCompat.resize(parent, width, height);
        UiHost.reattach(ui);
    }

    @Override
    protected void draw(Gfx g, int mx, int my, float delta) {
        if (parent != null) SocialCompat.renderBehind(parent, g.g, delta);
        UiHost.begin(ui, g, mx, my, delta);
        float o = open.to(1f);
        g.rect(0, 0, width, height, Theme.alpha(0xB4000000, o));
        int w = Math.min(280, width - 24);
        if (linesW != w) {
            linesW = w;
            lines = TextWrap.wrap(g, text == null ? "" : text, w - 24, 6);
        }
        int h = 44 + lines.size() * 10 + 26;
        int x = (width - w) / 2;
        int y = (height - h) / 2 + Math.round((1f - o) * 6);
        g.rect(x, y, w, h, Theme.SURFACE);
        g.outline(x - 1, y - 1, w + 2, h + 2, Theme.LINE2);
        g.rect(x, y, w, 2, Theme.BRAND);
        g.text(g.clip(heading, w - 24), x + 12, y + 12, Theme.TEXT);
        int ly = y + 28;
        for (String l : lines) {
            g.text(l, x + 12, ly, Theme.MUTED);
            ly += 10;
        }
        int bw = Math.max(70, g.width(confirm) + 20);
        int cw = Math.max(70, g.width(Lang.tr("social.action.cancel")) + 20);
        int by = y + h - 26;
        if (Widgets.button(ui, this, x + w - 12 - bw, by, bw, 16, confirm, Widgets.Style.PRIMARY)) yes();
        if (Widgets.button(ui, heading, x + w - 12 - bw - 6 - cw, by, cw, 16, Lang.tr("social.action.cancel"), Widgets.Style.SECONDARY)) {
            onClose();
        }
        if (o < 1f) g.rect(x - 1, y - 1, w + 2, h + 2, Theme.alpha(Theme.SURFACE, 1f - o));
        UiHost.end(ui);
    }

    private void yes() {
        ClientCompat.openScreen(parent);
        onYes.run();
    }

    @Override
    protected boolean click(double x, double y, int button) {
        UiHost.click(ui, x, y, button);
        return true;
    }

    @Override
    protected boolean release(double x, double y, int button) {
        UiHost.release(ui);
        return false;
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        return true;
    }

    @Override
    protected boolean key(int keyCode) {
        if (keyCode == Keys.ENTER) {
            yes();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        ClientCompat.openScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return parent != null && parent.isPauseScreen();
    }
}
