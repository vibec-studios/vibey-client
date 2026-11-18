package dev.henny.vibeyclient.module.media.video.ui;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.module.media.video.Video;
import dev.henny.vibeyclient.module.media.video.player.Ffmpeg;
import dev.henny.vibeyclient.module.media.video.web.McefInstaller;
import dev.henny.vibeyclient.module.media.video.web.Web;
import dev.henny.vibeyclient.ui.CardScreen;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Asks before anything is downloaded: what it is, how big, where from, which licence, where it goes. Nothing
 * happens until the user presses the download button; progress and errors are shown here.
 */
public final class SetupScreen extends CardScreen {
    public enum Need { FFMPEG, BROWSER_MOD, BROWSER_NATIVES }

    private enum Step { ASK, LOOKUP, DOWNLOAD, DONE, FAILED }

    private final Need need;
    private volatile Step step = Step.ASK;
    private volatile float progress;
    private volatile String error;
    private volatile String sizeText;

    public SetupScreen(Screen parent, Need need) {
        super(Component.literal(Video.tr("setup.title")), parent, "video.");
        this.need = need;
        if (need == Need.FFMPEG && Ffmpeg.downloadable()) {
            sizeText = tr("setup.ffmpeg.size", "~80");
        }
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
        return switch (need) {
            case FFMPEG -> Ffmpeg.downloadable() ? "setup.ffmpeg" : "setup.ffmpegmanual";
            case BROWSER_MOD -> "setup.mod";
            case BROWSER_NATIVES -> "setup.natives";
        };
    }

    @Override
    protected void drawHeader(Gfx g, int x, int y, int w) {
        title(g, tr(key() + ".title"), x + PAD, y + 13);
        closeButton(g, x, y, w);
    }

    @Override
    protected int drawBody(Gfx g, int x, int y, int w) {
        for (String line : wrapAll(g, tr(key() + ".what"), w)) {
            g.text(line, x, y, TEXT);
            y += 11;
        }
        y += 6;
        List<String[]> facts = new ArrayList<>();
        switch (need) {
            case FFMPEG -> {
                if (Ffmpeg.downloadable()) {
                    facts.add(new String[] {tr("setup.size"), sizeText});
                    facts.add(new String[] {tr("setup.from"), Ffmpeg.SOURCE});
                    facts.add(new String[] {tr("setup.license"), Ffmpeg.LICENSE});
                    facts.add(new String[] {tr("setup.to"), "vibeyclient/ffmpeg/"});
                } else {
                    facts.add(new String[] {"Linux", "sudo apt install ffmpeg  /  dnf  /  pacman -S ffmpeg"});
                    facts.add(new String[] {"macOS", "brew install ffmpeg"});
                }
            }
            case BROWSER_MOD -> {
                facts.add(new String[] {tr("setup.size"), tr("setup.mod.size", Web.NATIVES_MB)});
                facts.add(new String[] {tr("setup.from"), Web.MOD_SOURCE});
                facts.add(new String[] {"", Web.NATIVES_SOURCE});
                facts.add(new String[] {tr("setup.license"), Web.MOD_LICENSE + tr("setup.mod.license")});
                facts.add(new String[] {tr("setup.to"), "mods/, config/mcef-modern/"});
            }
            case BROWSER_NATIVES -> {
                facts.add(new String[] {tr("setup.size"), "~" + Web.NATIVES_MB + " MB"});
                facts.add(new String[] {tr("setup.from"), Web.NATIVES_SOURCE});
                facts.add(new String[] {tr("setup.license"), tr("setup.natives.license")});
                facts.add(new String[] {tr("setup.to"), "config/mcef-modern/"});
            }
        }
        for (String[] f : facts) {
            g.text(f[0], x, y, MUTED);
            g.text(g.clip(f[1], w - 70), x + 70, y, TEXT);
            y += 12;
        }
        y += 8;
        Step st = step;
        if (need == Need.BROWSER_NATIVES && st == Step.DOWNLOAD) {
            Web.State ws = Web.state(true);
            float p = Web.progress();
            progress = p < 0 ? 0 : p / 100f;
            if (ws == Web.State.READY) step = Step.DONE;
            if (ws == Web.State.FAILED) {
                error = Web.failure();
                step = Step.FAILED;
            }
        }
        switch (st) {
            case LOOKUP -> {
                g.text(tr("setup.lookup") + dots(), x, y, MUTED);
                y += 14;
            }
            case DOWNLOAD -> {
                bar(g, x, y, w, 6, progress, null);
                y += 10;
                String stage = need == Need.BROWSER_NATIVES ? Web.stage().toLowerCase(Locale.ROOT) : "";
                g.text(Math.round(progress * 100) + " %  " + stage, x, y, MUTED);
                y += 14;
            }
            case DONE -> {
                for (String line : wrapAll(g, tr(need == Need.BROWSER_MOD ? "setup.done.restart" : "setup.done"), w)) {
                    g.text(line, x, y, GREEN);
                    y += 11;
                }
            }
            case FAILED -> {
                for (String line : wrapAll(g, tr("setup.failed", error == null ? "?" : error), w)) {
                    g.text(line, x, y, RED);
                    y += 11;
                }
            }
            default -> {
                g.text(g.clip(tr("setup.nothing"), w), x, y, DIM);
                y += 12;
            }
        }
        return y + 4;
    }

    private static List<String> wrapAll(Gfx g, String text, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (g.width(next) > width && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    @Override
    protected void drawFooter(Gfx g, int x, int y, int w) {
        int by = y + 11;
        int right = x + w - PAD;
        Step st = step;
        if (st == Step.DONE) {
            buttonRight(g, right, by, 18, tr("setup.close"), Style.PRIMARY, this::onClose);
            return;
        }
        boolean busy = st == Step.LOOKUP || st == Step.DOWNLOAD;
        boolean manual = need == Need.FFMPEG && !Ffmpeg.downloadable();
        String action = manual ? tr("setup.check") : need == Need.BROWSER_NATIVES ? tr("setup.continue") : tr("setup.download");
        int left = buttonRight(g, right, by, 18, action, busy ? Style.DISABLED : Style.PRIMARY, this::go);
        buttonRight(g, left - 6, by, 18, tr("setup.later"), Style.SECONDARY, this::onClose);
    }

    /** The download/continue button (public for the dev driver). */
    public void go() {
        error = null;
        switch (need) {
            case FFMPEG -> {
                if (!Ffmpeg.downloadable()) {
                    Ffmpeg.refresh();
                    if (Ffmpeg.available()) step = Step.DONE;
                    else {
                        error = tr("setup.stillmissing");
                        step = Step.FAILED;
                    }
                    return;
                }
                step = Step.LOOKUP;
                background(() -> {
                    Ffmpeg.Asset a = Ffmpeg.lookup();
                    sizeText = tr("setup.ffmpeg.size", String.valueOf(Math.round(a.size() / 1_048_576f)));
                    step = Step.DOWNLOAD;
                    Ffmpeg.install(a, f -> progress = f);
                    step = Step.DONE;
                });
            }
            case BROWSER_MOD -> {
                step = Step.LOOKUP;
                background(() -> {
                    McefInstaller.Release rel = McefInstaller.lookup();
                    step = Step.DOWNLOAD;
                    McefInstaller.install(rel, f -> progress = f);
                    Video.get().setBrowserConsent(); // they agreed to the Chromium download as well
                    step = Step.DONE;
                });
            }
            case BROWSER_NATIVES -> {
                Video.get().setBrowserConsent();
                Web.start();
                step = Step.DOWNLOAD;
            }
        }
    }

    private interface Job {
        void run() throws Exception;
    }

    private void background(Job job) {
        Thread t = new Thread(() -> {
            try {
                job.run();
            } catch (Exception e) {
                Vibey.LOG.warn("Video setup failed", e);
                error = e.getMessage() != null ? e.getMessage() : e.toString();
                step = Step.FAILED;
            }
        }, "Vibey video setup");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void onClose() {
        if (step == Step.DOWNLOAD && need != Need.BROWSER_NATIVES) return; // finish first
        Minecraft.getInstance().setScreen(parent);
    }
}
