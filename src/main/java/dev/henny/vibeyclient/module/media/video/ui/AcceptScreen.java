package dev.henny.vibeyclient.module.media.video.ui;

import dev.henny.vibeyclient.module.media.video.MediaScreen;
import dev.henny.vibeyclient.module.media.video.Video;
import dev.henny.vibeyclient.ui.CardScreen;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** "Alex shared a screen – load it?" Loading connects the game to that address, so we ask first. */
public final class AcceptScreen extends CardScreen {
    private final MediaScreen s;

    public AcceptScreen(Screen parent, MediaScreen screen) {
        super(Component.literal(Video.tr("accept.title", screen.ownerName)), parent, "media.video.");
        this.s = screen;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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
        return 40;
    }

    @Override
    protected void drawHeader(Gfx g, int x, int y, int w) {
        title(g, g.clip(tr("accept.title", s.ownerName), w - 40), x + PAD, y + 13);
        closeButton(g, x, y, w);
    }

    @Override
    protected int drawBody(Gfx g, int x, int y, int w) {
        g.text(g.clip(s.source.uri(), w), x, y, TEXT);
        y += 14;
        for (String line : wrap(g, tr("accept.body", s.source.label()), w)) {
            g.text(line, x, y, MUTED);
            y += 11;
        }
        y += 4;
        g.text(g.clip(tr("accept.setting"), w), x, y, DIM);
        return y + 14;
    }

    @Override
    protected void drawFooter(Gfx g, int x, int y, int w) {
        int by = y + 11;
        int left = buttonRight(g, x + w - PAD, by, 18, tr("accept.load"), Style.PRIMARY, () -> {
            s.accepted = true;
            Video v = Video.get();
            SetupScreen.Need need = v.missing(s);
            Minecraft.getInstance().setScreen(need != null ? new SetupScreen(null, need) : null);
        });
        buttonRight(g, left - 6, by, 18, tr("accept.no"), Style.SECONDARY, this::onClose);
    }
}
