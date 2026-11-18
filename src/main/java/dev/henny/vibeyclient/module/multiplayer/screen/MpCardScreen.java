package dev.henny.vibeyclient.module.multiplayer.screen;

import dev.henny.vibeyclient.module.multiplayer.T;
import dev.henny.vibeyclient.module.multiplayer.host.HostSession;
import dev.henny.vibeyclient.ui.CardScreen;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The card look of the host screens plus their "● Online" pill. */
abstract class MpCardScreen extends CardScreen {
    protected MpCardScreen(Component title, Screen parent) {
        super(title, parent, "multiplayer.");
    }

    /** "● Online" pill, right-aligned to {@code right}. */
    protected void statusPill(Gfx g, int right, int y) {
        HostSession.Status st = HostSession.status();
        int color;
        String label;
        switch (st) {
            case ONLINE -> {
                color = GREEN;
                label = T.get("status.online");
            }
            case CONNECTING -> {
                color = AMBER;
                label = T.get("status.connecting");
            }
            case RECONNECTING -> {
                color = AMBER;
                label = T.get("status.reconnecting");
            }
            case ERROR -> {
                color = RED;
                label = T.get("status.error");
            }
            default -> {
                color = DIM;
                label = T.get("status.off");
            }
        }
        int pw = g.width(label) + 20;
        int px = right - pw;
        g.rect(px, y, pw, 14, alpha(color, 0.12f));
        g.outline(px, y, pw, 14, alpha(color, 0.45f));
        float pulse = st == HostSession.Status.CONNECTING || st == HostSession.Status.RECONNECTING
                ? 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 180.0) : 1f;
        if (st == HostSession.Status.ONLINE) {
            float ring = (System.currentTimeMillis() % 1600) / 1600f;
            int r = 2 + Math.round(ring * 3);
            g.outline(px + 8 - r, y + 7 - r, r * 2, r * 2, alpha(color, (1f - ring) * 0.5f));
        }
        g.rect(px + 6, y + 5, 4, 4, alpha(color, pulse));
        g.text(label, px + 14, y + 3, st == HostSession.Status.OFF ? MUTED : TEXT);
    }
}
