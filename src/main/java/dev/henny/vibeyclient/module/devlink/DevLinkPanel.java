package dev.henny.vibeyclient.module.devlink;

import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Panel;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;
import dev.henny.vibeyclient.ui.screen.SettingsView;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Settings page of the devlink module: server status, token (masked, copy, regenerate), how to connect, settings. */
final class DevLinkPanel implements Panel {
    private static final Object SHOW = new Object(), COPY = new Object(), REGEN = new Object();

    private final DevLinkModule module;
    private final ScrollArea scroll = new ScrollArea();
    private boolean showToken;
    private long copiedAt;
    private long regenArmedAt;

    DevLinkPanel(DevLinkModule module) {
        this.module = module;
    }

    @Override
    public void onHide() {
        showToken = false;
        regenArmedAt = 0;
    }

    @Override
    public void draw(Ui ui, int x, int y, int w, int h) {
        Gfx g = ui.g;
        int pad = Theme.PAD + 4;
        int cx = x + pad;
        int cw = w - pad * 2;
        int cy = scroll.begin(ui, x, y, w, h) + pad;

        cy = dev.henny.vibeyclient.ui.screen.ModulesPanel.intro(ui, module, cx, cy, cw);
        cy = status(ui, cx, cy, cw) + 10;
        cy = token(ui, cx, cy, cw) + 10;

        Widgets.caption(g, module.tr("panel.howTo"), cx, cy);
        cy += 12;
        for (String key : new String[] {"panel.hint1", "panel.hint2", "panel.hint3"}) {
            for (String line : wrap(g, module.tr(key), cw)) {
                g.text(line, cx, cy, Theme.MUTED);
                cy += 11;
            }
            cy += 2;
        }
        cy += 10;

        cy = SettingsView.draw(ui, module.settings(), cx, cy, cw, module.isEnabled()) + pad;
        scroll.end(ui, cy);
    }

    private int status(Ui ui, int x, int y, int w) {
        Gfx g = ui.g;
        int bh = 38;
        DevLinkModule.State state = module.state();
        int color;
        String chip;
        String line;
        switch (state) {
            case LISTENING -> {
                color = Theme.OK;
                chip = module.tr("panel.state.listening");
                line = module.tr("panel.listeningOn", "127.0.0.1:" + module.listeningPort());
            }
            case PORT_IN_USE -> {
                color = Theme.DANGER;
                chip = module.tr("panel.state.error");
                line = module.tr("panel.portInUse", module.listeningPort());
            }
            case FAILED -> {
                color = Theme.DANGER;
                chip = module.tr("panel.state.error");
                line = module.tr("panel.failed", module.failure());
            }
            default -> {
                color = Theme.MUTED;
                chip = module.tr("panel.state.off");
                line = module.tr(!module.isEnabled() ? "panel.offModule" : "panel.offCreator");
            }
        }
        g.rect(x, y, w, bh, Theme.SURFACE2);
        g.vline(x, y, bh, color);
        Widgets.caption(g, module.tr("panel.status"), x + 8, y + 6);
        int chipW = Widgets.chip(g, chip, x + w - 8 - (g.width(chip) + 8), y + 4, color);
        g.text(g.clip(line, w - 24 - chipW), x + 8, y + 18, Theme.TEXT);
        if (state == DevLinkModule.State.LISTENING) {
            int n = module.clients();
            String clients = n == 0 ? module.tr("panel.clients.none") : module.tr(n == 1 ? "panel.clients.one" : "panel.clients.many", n);
            g.text(g.clip(clients, w - 16), x + 8, y + 28, n > 0 ? Theme.OK : Theme.DIM);
        }
        return y + bh;
    }

    private int token(Ui ui, int x, int y, int w) {
        Gfx g = ui.g;
        Widgets.caption(g, module.tr("panel.token"), x, y);
        y += 12;
        String token = module.token();
        g.rect(x, y, w, 18, Theme.SURFACE);
        g.outline(x, y, w, 18, Theme.LINE2);
        String shown = showToken ? token : "*".repeat(Math.min(token.length(), 32));
        g.text(g.clip(shown, w - 12), x + 6, y + 5, showToken ? Theme.TEXT : Theme.DIM);
        y += 22;

        long now = System.currentTimeMillis();
        boolean armed = now - regenArmedAt < 3000;
        String show = module.tr(showToken ? "panel.hide" : "panel.show");
        String copy = module.tr(now - copiedAt < 1500 ? "panel.copied" : "panel.copy");
        String regen = module.tr(armed ? "panel.regenerateConfirm" : "panel.regenerate");
        int bx = x;
        int bw = Math.max(50, g.width(show) + 16);
        if (Widgets.button(ui, SHOW, bx, y, bw, 16, show, Widgets.Style.SECONDARY)) showToken = !showToken;
        bx += bw + 4;
        bw = Math.max(70, g.width(module.tr("panel.copy")) + 16);
        if (Widgets.button(ui, COPY, bx, y, bw, 16, copy, Widgets.Style.PRIMARY)) {
            try {
                Minecraft.getInstance().keyboardHandler.setClipboard(token);
                copiedAt = now;
            } catch (RuntimeException ignored) {
                // no clipboard (headless)
            }
        }
        bx += bw + 4;
        bw = Math.max(90, Math.max(g.width(module.tr("panel.regenerate")), g.width(module.tr("panel.regenerateConfirm"))) + 16);
        if (Widgets.button(ui, REGEN, bx, y, bw, 16, regen, armed ? Widgets.Style.DANGER : Widgets.Style.SECONDARY)) {
            if (armed) {
                module.regenerateToken();
                regenArmedAt = 0;
            } else {
                regenArmedAt = now;
            }
        }
        y += 20;
        for (String line : wrap(g, module.tr("panel.tokenHint"), w)) {
            g.text(line, x, y, Theme.DIM);
            y += 11;
        }
        return y;
    }

    /** Greedy word wrap to {@code width} pixels. */
    private static List<String> wrap(Gfx g, String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (g.width(candidate) <= width || line.isEmpty()) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(g.clip(line.toString(), width));
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) lines.add(g.clip(line.toString(), width));
        return lines;
    }
}
