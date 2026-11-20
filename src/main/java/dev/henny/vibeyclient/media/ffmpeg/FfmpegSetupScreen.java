package dev.henny.vibeyclient.media.ffmpeg;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.CardScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.TextWrap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class FfmpegSetupScreen extends CardScreen {
    private enum Step { ASK, LOOKUP, DOWNLOAD, DONE, FAILED }

    private final FfmpegPurpose purpose;
    private volatile Step step = Step.ASK;
    private volatile float progress;
    private volatile String error;
    private volatile String sizeText;

    public FfmpegSetupScreen(Screen parent, FfmpegPurpose purpose) {
        super(heading(), parent, "media.ffmpeg.");
        this.purpose = purpose;
        if (Ffmpeg.downloadable()) sizeText = Lang.tr("media.ffmpeg.size", "~80");
    }

    private static Component heading() {
        Lang.addFile("media.ffmpeg");
        return Component.literal(Lang.tr("media.ffmpeg.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected int cardWidth() {
        return 340;
    }

    @Override
    protected int headerHeight() {
        return 34;
    }

    @Override
    protected int footerHeight() {
        return 40;
    }

    private String key() {
        return purpose.key + (Ffmpeg.downloadable() ? "" : ".manual");
    }

    @Override
    protected void drawHeader(Gfx g, int x, int y, int w) {
        title(g, tr(key() + ".title"), x + PAD, y + 13);
        closeButton(g, x, y, w);
    }

    @Override
    protected int drawBody(Gfx g, int x, int y, int w) {
        y = lines(g, tr(key() + ".what"), x, y, w, TEXT) + 6;
        for (String[] f : facts()) {
            g.text(f[0], x, y, MUTED);
            g.text(g.clip(f[1], w - 70), x + 70, y, TEXT);
            y += 12;
        }
        y += 8;
        return drawStatus(g, x, y, w) + 4;
    }

    private List<String[]> facts() {
        List<String[]> facts = new ArrayList<>();
        if (Ffmpeg.downloadable()) {
            facts.add(new String[] {Lang.tr("media.ffmpeg.fact.size"), sizeText});
            facts.add(new String[] {Lang.tr("media.ffmpeg.fact.from"), Ffmpeg.SOURCE});
            facts.add(new String[] {Lang.tr("media.ffmpeg.fact.license"), Ffmpeg.LICENSE});
            facts.add(new String[] {Lang.tr("media.ffmpeg.fact.to"), "vibeyclient/ffmpeg/"});
        } else {
            facts.add(new String[] {"Linux", "sudo apt install ffmpeg  /  dnf  /  pacman -S ffmpeg"});
            facts.add(new String[] {"macOS", "brew install ffmpeg"});
        }
        return facts;
    }

    private int drawStatus(Gfx g, int x, int y, int w) {
        switch (step) {
            case LOOKUP -> {
                g.text(Lang.tr("media.ffmpeg.lookup") + dots(), x, y, MUTED);
                return y + 14;
            }
            case DOWNLOAD -> {
                bar(g, x, y, w, 6, progress, null);
                g.text(Math.round(progress * 100) + " %", x, y + 10, MUTED);
                return y + 24;
            }
            case DONE -> {
                return lines(g, tr(purpose.key + ".done"), x, y, w, GREEN);
            }
            case FAILED -> {
                return lines(g, Lang.tr("media.ffmpeg.failed", error == null ? "?" : error), x, y, w, RED);
            }
            default -> {
                g.text(g.clip(Lang.tr("media.ffmpeg.nothing"), w), x, y, DIM);
                return y + 12;
            }
        }
    }

    private static int lines(Gfx g, String text, int x, int y, int w, int color) {
        for (String line : TextWrap.all(g, text, w)) {
            g.text(line, x, y, color);
            y += 11;
        }
        return y;
    }

    @Override
    protected void drawFooter(Gfx g, int x, int y, int w) {
        int by = y + 11;
        int right = x + w - PAD;
        if (step == Step.DONE) {
            buttonRight(g, right, by, 18, Lang.tr("media.ffmpeg.close"), Style.PRIMARY, this::onClose);
            return;
        }
        boolean busy = step == Step.LOOKUP || step == Step.DOWNLOAD;
        String action = Ffmpeg.downloadable() ? Lang.tr("media.ffmpeg.download") : Lang.tr("media.ffmpeg.check");
        int left = buttonRight(g, right, by, 18, action, busy ? Style.DISABLED : Style.PRIMARY, this::go);
        buttonRight(g, left - 6, by, 18, Lang.tr("media.ffmpeg.later"), Style.SECONDARY, this::onClose);
    }

    public void go() {
        error = null;
        if (!Ffmpeg.downloadable()) {
            Ffmpeg.refresh();
            if (Ffmpeg.available()) {
                step = Step.DONE;
            } else {
                error = Lang.tr("media.ffmpeg.stillmissing");
                step = Step.FAILED;
            }
            return;
        }
        step = Step.LOOKUP;
        Thread t = new Thread(this::install, "Vibey ffmpeg setup");
        t.setDaemon(true);
        t.start();
    }

    private void install() {
        try {
            FfmpegAsset a = FfmpegRelease.lookup();
            sizeText = Lang.tr("media.ffmpeg.size", String.valueOf(Math.round(a.size() / 1_048_576f)));
            step = Step.DOWNLOAD;
            FfmpegInstaller.install(a, f -> progress = f);
            step = Step.DONE;
        } catch (Exception e) {
            Vibey.LOG.warn("FFmpeg setup failed", e);
            error = e.getMessage() != null ? e.getMessage() : e.toString();
            step = Step.FAILED;
        }
    }

    @Override
    public void onClose() {
        if (step == Step.DOWNLOAD) return;
        Minecraft.getInstance().setScreen(parent);
    }
}
