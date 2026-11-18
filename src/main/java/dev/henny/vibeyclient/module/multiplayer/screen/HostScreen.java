package dev.henny.vibeyclient.module.multiplayer.screen;

import dev.henny.vibeyclient.module.multiplayer.MpCompat;

import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.module.multiplayer.host.Guests;
import dev.henny.vibeyclient.module.multiplayer.host.HostConfig;
import dev.henny.vibeyclient.module.multiplayer.host.HostSession;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import dev.henny.vibeyclient.module.multiplayer.T;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

import dev.henny.vibeyclient.module.multiplayer.host.PlayLinks;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The main host window, kept as small as possible: your address, whether you are online, start/stop. Everything
 * else (players, whitelist, bans, world settings) lives in {@link AdminScreen}.
 */
public final class HostScreen extends MpCardScreen {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private long copiedAt;
    private long linkCopiedAt;
    /** Last drawn "Copy invite link" button (x, y, w, h in GUI pixels) or null; for the gametest's click. */
    private volatile int[] inviteButton;

    public HostScreen(Screen parent) {
        super(T.text("title"), parent);
        PlayLinks.refreshInstance();
    }

    @Override
    public void removed() {
        HostConfig.save();
    }

    @Override
    protected int cardWidth() {
        return 320;
    }

    @Override
    protected int headerHeight() {
        return 34;
    }

    @Override
    protected int footerHeight() {
        return 38;
    }

    @Override
    protected void drawHeader(Gfx g, int x, int y, int w) {
        title(g, T.get("title"), x + PAD, y + 13);
        int closeLeft = closeButton(g, x, y, w);
        statusPill(g, closeLeft - 6, y + 10);
    }

    @Override
    protected int drawBody(Gfx g, int x, int y, int w) {
        y = drawAddress(g, x, y, w);
        if (PlayLinks.visible() && HostSession.status() != HostSession.Status.ERROR) {
            y += 6;
            y = drawInvite(g, x, y, w);
        }
        y += 8;
        g.text(g.clip(summary(), w), x, y, DIM);
        y += 12;
        int manual = PlayLinks.manualMods();
        if (manual > 0) {
            y += 2;
            for (String l : wrap(g, T.get(manual == 1 ? "link.manual.one" : "link.manual", manual), w)) {
                g.text(l, x, y, AMBER);
                y += 10;
            }
        }
        return y;
    }

    private int drawAddress(Gfx g, int x, int y, int w) {
        HostSession.Status st = HostSession.status();
        if (st == HostSession.Status.ERROR) {
            List<String> lines = wrap(g, HostSession.error(), w - 20);
            int h = 22 + lines.size() * 10 + 12;
            g.rect(x, y, w, h, 0xFF2A1416);
            g.outline(x, y, w, h, 0xFF4A2024);
            g.text(T.get("status.error"), x + 10, y + 8, RED);
            int ly = y + 21;
            for (String l : lines) {
                g.text(l, x + 10, ly, TEXT);
                ly += 10;
            }
            g.text(g.clip(T.get("error.hint"), w - 20), x + 10, ly + 2, MUTED);
            return y + h;
        }

        int h = 44;
        g.rect(x, y, w, h, SURFACE);
        g.text(T.get("address"), x + 10, y + 8, MUTED);

        String address = HostSession.address();
        boolean live = st == HostSession.Status.ONLINE;
        boolean pending = st == HostSession.Status.CONNECTING || st == HostSession.Status.RECONNECTING;

        int btnW = 0;
        if (!address.isEmpty()) {
            boolean copied = System.currentTimeMillis() - copiedAt < 1500;
            String bl = T.get(copied ? "copied" : "copy");
            btnW = g.width(bl) + 16;
            int bx = x + w - 8 - btnW;
            int by = y + 13;
            boolean hov = hover(bx, by, btnW, 18);
            g.rect(bx, by, btnW, 18, copied ? 0xFF1F3A28 : (hov ? ACCENT_HOVER : ACCENT));
            g.textCenter(bl, bx + btnW / 2, by + 5, copied ? GREEN : 0xFFFFFFFF);
            hit(bx, by, btnW, 18, () -> {
                this.minecraft.keyboardHandler.setClipboard(address);
                copiedAt = System.currentTimeMillis();
            });
        }

        int textW = w - 20 - (btnW > 0 ? btnW + 8 : 0);
        if (address.isEmpty()) {
            String t = pending ? T.get("status.connecting") + dots() : T.get("address.none");
            g.text(g.clip(t, textW), x + 10, y + 21, pending ? TEXT : DIM);
        } else {
            // Bold glyphs are one pixel wider than the plain ones the clip measures.
            g.text(Component.literal(g.clip(address, textW - address.length())).withStyle(ChatFormatting.BOLD), x + 10, y + 21, live ? TEXT : MUTED);
        }
        String sub;
        if (live) sub = T.get("address.hint");
        else if (pending) sub = T.get("address.pending");
        else sub = T.get(address.isEmpty() ? "address.first" : "address.offline");
        String notice = HostSession.notice();
        if (live && !notice.isEmpty()) g.text(g.clip(notice, textW), x + 10, y + 32, AMBER);
        else g.text(g.clip(sub, textW), x + 10, y + 32, DIM);
        return y + h;
    }

    /** The invite link ("play with me"): link + copy button, or what is missing for one. */
    private int drawInvite(Gfx g, int x, int y, int w) {
        int h = 44;
        g.rect(x, y, w, h, SURFACE);
        g.text(T.get("link.title"), x + 10, y + 8, MUTED);
        PlayLinks.State st = PlayLinks.state();
        boolean hosting = HostSession.isHosting();
        PlayLinks.Link link = PlayLinks.link();

        int btnW = 0;
        if (hosting && link != null) {
            boolean copied = System.currentTimeMillis() - linkCopiedAt < 1500;
            String bl = T.get(copied ? "copied" : "link.copy");
            btnW = g.width(bl) + 16;
            int bx = x + w - 8 - btnW;
            int by = y + 13;
            boolean hov = hover(bx, by, btnW, 18);
            g.rect(bx, by, btnW, 18, copied ? 0xFF1F3A28 : (hov ? ACCENT_HOVER : ACCENT));
            g.textCenter(bl, bx + btnW / 2, by + 5, copied ? GREEN : 0xFFFFFFFF);
            hit(bx, by, btnW, 18, () -> {
                if (PlayLinks.copy()) linkCopiedAt = System.currentTimeMillis();
            });
            inviteButton = new int[]{bx, by, btnW, 18};
        } else if (hosting && (st == PlayLinks.State.ERROR)) {
            String bl = T.get("button.retry");
            btnW = g.width(bl) + 16;
            int bx = x + w - 8 - btnW;
            int by = y + 13;
            boolean hov = hover(bx, by, btnW, 18);
            g.rect(bx, by, btnW, 18, hov ? SURFACE_HOVER : SURFACE_2);
            g.outline(bx, by, btnW, 18, CARD_LINE);
            g.textCenter(bl, bx + btnW / 2, by + 5, TEXT);
            hit(bx, by, btnW, 18, PlayLinks::retry);
        }
        if (!(hosting && link != null)) inviteButton = null;
        int textW = w - 20 - (btnW > 0 ? btnW + 8 : 0);
        String main;
        String sub;
        int mainColor = TEXT;
        int subColor = DIM;
        if (!hosting) {
            main = T.get("link.none");
            sub = T.get("link.none.hint");
            mainColor = DIM;
        } else if (link != null) {
            main = link.url().replaceFirst("^https?://", "");
            sub = link.expiresAt() > 0 ? T.get("link.expires", TIME.format(Instant.ofEpochMilli(link.expiresAt()).atZone(ZoneId.systemDefault())))
                    : T.get("link.hint");
        } else {
            switch (st) {
                case SIGNED_OUT -> {
                    main = T.get("link.signin");
                    sub = T.get("link.signin.hint");
                    mainColor = AMBER;
                }
                case RATE_LIMITED -> {
                    long min = Math.max(1, (PlayLinks.retryAt() - System.currentTimeMillis() + 59_999) / 60_000);
                    main = T.get("link.ratelimited");
                    sub = T.get("link.ratelimited.hint", min);
                    mainColor = AMBER;
                }
                case ERROR -> {
                    main = T.get("link.error");
                    sub = PlayLinks.error();
                    mainColor = RED;
                }
                default -> {
                    main = T.get("link.creating") + dots();
                    sub = T.get("link.hint");
                }
            }
        }
        if (link != null && hosting) {
            g.text(Component.literal(g.clip(main, textW - main.length())).withStyle(ChatFormatting.BOLD), x + 10, y + 21, TEXT);
        } else {
            g.text(g.clip(main, textW), x + 10, y + 21, mainColor);
        }
        g.text(g.clip(sub, textW), x + 10, y + 32, subColor);
        return y + h;
    }

    /** Bounds of the "Copy invite link" button as last drawn, or null (tests). */
    public int[] inviteButtonBounds() {
        return inviteButton;
    }

    /** One line: players · game mode · whitelist. */
    private String summary() {
        HostConfig cfg = HostConfig.get();
        List<String> parts = new ArrayList<>();
        if (HostSession.isHosting()) {
            parts.add(T.get("summary.players", Guests.snapshot().size(), cfg.maxPlayers));
        }
        parts.add(GameType.byName(cfg.gameMode, GameType.SURVIVAL).getShortDisplayName().getString());
        if (cfg.cheats) parts.add(T.get("cheats"));
        if (cfg.whitelistEnabled) parts.add(T.get("summary.whitelist"));
        if (cfg.forceMods) parts.add(T.get("forcemods"));
        return String.join("  ·  ", parts);
    }

    @Override
    protected void drawFooter(Gfx g, int x, int bottom, int w) {
        int y = bottom + 8;
        int bx = x + PAD;
        int bw = w - PAD * 2;
        int manageW = Math.max(80, g.width(T.get("manage")) + 24);
        button(g, bx, y, manageW, 20, T.get("manage"), Style.SECONDARY,
                () -> MpCompat.openScreen(new AdminScreen(this)));

        int px = bx + manageW + 6;
        int pw = bw - manageW - 6;
        if (!HostSession.canHost()) {
            button(g, px, y, pw, 20, T.get("button.noworld"), Style.DISABLED, null);
        } else if (HostSession.isHosting()) {
            button(g, px, y, pw, 20, T.get("button.stop"), Style.DANGER, HostSession::stop);
        } else {
            String key = HostSession.status() == HostSession.Status.ERROR ? "button.retry" : "button.start";
            button(g, px, y, pw, 20, T.get(key), Style.PRIMARY, () -> {
                HostConfig.save();
                HostSession.start();
            });
        }
    }
}
