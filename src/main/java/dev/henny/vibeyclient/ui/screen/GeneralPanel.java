package dev.henny.vibeyclient.ui.screen;

import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.net.NetLimits;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Panel;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;
import dev.henny.vibeyclient.ui.title.BackendStatus;
import net.minecraft.client.Minecraft;

/**
 * "General" tab: the settings of the whole client (language, menu size, animations, online features, the menu key)
 * in plain words, the connection status, and in the collapsed advanced area the server address and the technical
 * facts (version, loader, protocol) with a "copy" button for bug reports. Notices (built-in and the backend's
 * announcements) are cards at the top until dismissed.
 */
public final class GeneralPanel implements Panel {
    private static final Object COPY = new Object();

    private final ScrollArea scroll = new ScrollArea();
    private long copiedAt;

    @Override
    public void onShow() {
        BackendStatus.refresh();
    }

    @Override
    public void draw(Ui ui, int x, int y, int w, int h) {
        Gfx g = ui.g;
        int pad = Theme.PAD + 4;
        int cx = x + pad, cw = w - pad * 2;
        int cy = scroll.begin(ui, x, y, w, h) + pad;
        g.bold(Lang.tr("core.tab.general"), cx, cy, Theme.TEXT);
        cy += 13;
        g.text(g.clip(Lang.tr("core.general.description"), cw), cx, cy, Theme.MUTED);
        cy += 18;
        cy = dev.henny.vibeyclient.ui.notice.NoticeCards.draw(ui, cx, cy, cw);
        cy = SettingsView.drawBasic(ui, GlobalSettings.GROUP, cx, cy, cw, true, "online", (u, sx, sy, sw) -> status(u, sx, sy + 6, sw) + 2);
        cy = SettingsView.drawAdvanced(ui, GlobalSettings.GROUP, cx, cy, cw, true, this::info);
        scroll.end(ui, cy + pad);
    }

    /** "● Connected to Vibe · 3 friends online" under the settings (only while online features are on). */
    private int status(Ui ui, int x, int y, int w) {
        Gfx g = ui.g;
        if (!GlobalSettings.online()) {
            g.text(g.clip(Lang.tr("core.general.offlineHint"), w), x + 1, y, Theme.DIM);
            return y + 10;
        }
        BackendStatus.State st = BackendStatus.state();
        int dot = BackendStatus.color(st);
        g.rect(x + 1, y + 1, 5, 5, dot);
        String label = BackendStatus.label();
        g.text(g.clip(label, w - 12), x + 11, y, Theme.MUTED);
        ui.tooltip(st, x, y - 2, Math.min(w, g.width(label) + 14), 12,
                Lang.tr("core.status.tip." + st.name().toLowerCase(java.util.Locale.ROOT)));
        return y + 10;
    }

    /** Technical facts in the advanced area. */
    private int info(Ui ui, int x, int y, int w) {
        Gfx g = ui.g;
        String[][] rows = rows();
        int rh = 16;
        int h = rows.length * rh + 34;
        Widgets.card(g, x, y, w, h);
        Widgets.caption(g, Lang.tr("core.general.info"), x + 10, y + 8);
        int ry = y + 22;
        int keyW = 0;
        for (String[] r : rows) keyW = Math.max(keyW, g.width(r[0]));
        keyW = Math.min(keyW + 12, w / 2);
        for (String[] r : rows) {
            g.text(g.clip(r[0], keyW - 6), x + 10, ry + 4, Theme.DIM);
            g.text(g.clip(r[1], w - keyW - 20), x + 10 + keyW, ry + 4, Theme.TEXT);
            ry += rh;
        }
        long now = System.currentTimeMillis();
        String label = Lang.tr(now - copiedAt < 1500 ? "core.general.copied" : "core.general.copy");
        int bw = Math.max(70, g.width(Lang.tr("core.general.copy")) + 16);
        if (Widgets.button(ui, COPY, x + w - bw - 8, y + 5, bw, 14, label, Widgets.Style.SECONDARY)) {
            StringBuilder sb = new StringBuilder();
            for (String[] r : rows) sb.append(r[0]).append(": ").append(r[1]).append('\n');
            try {
                Minecraft.getInstance().keyboardHandler.setClipboard(sb.toString().trim());
                copiedAt = now;
            } catch (RuntimeException ignored) {
                // no clipboard (headless)
            }
        }
        return y + h;
    }

    private static String[][] rows() {
        return new String[][] {
                {Lang.tr("core.general.version"), BuildInfo.VERSION},
                {Lang.tr("core.general.loader"), Vibey.platform().loader() + " · Minecraft " + BuildInfo.MINECRAFT_LABEL},
                {Lang.tr("core.general.server"), GlobalSettings.backend()},
                {Lang.tr("core.general.connection"), GlobalSettings.online() ? BackendStatus.state().name().toLowerCase(java.util.Locale.ROOT) : "off"},
                {Lang.tr("core.general.protocol"), String.valueOf(NetLimits.PROTOCOL)},
                {Lang.tr("core.general.config"), SettingsGroup.configDir().toString()},
        };
    }
}
