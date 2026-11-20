package dev.henny.vibeyclient.module.media.video.ui;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.module.media.video.Video;
import dev.henny.vibeyclient.module.media.video.web.McefInstaller;
import dev.henny.vibeyclient.module.media.video.web.Web;
import dev.henny.vibeyclient.ui.CardScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.TextWrap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SetupScreen extends CardScreen {
    private enum Step { ASK, LOOKUP, DOWNLOAD, DONE, FAILED }

    private final SetupNeed need;
    private volatile Step step = Step.ASK;
    private volatile float progress;
    private volatile String error;

    SetupScreen(Screen parent, SetupNeed need) {
        super(Component.literal(Video.tr("setup.title")), parent, "media.video.");
        if (need == SetupNeed.FFMPEG) throw new IllegalArgumentException("FFmpeg has its own setup screen");
        this.need = need;
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
        return need == SetupNeed.BROWSER_MOD ? "setup.mod" : "setup.natives";
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
        pollBrowser();
        return drawStatus(g, x, y, w) + 4;
    }

    private List<String[]> facts() {
        List<String[]> facts = new ArrayList<>();
        if (need == SetupNeed.BROWSER_MOD) {
            facts.add(new String[] {tr("setup.size"), tr("setup.mod.size", Web.NATIVES_MB)});
            facts.add(new String[] {tr("setup.from"), Web.MOD_SOURCE});
            facts.add(new String[] {"", Web.NATIVES_SOURCE});
            facts.add(new String[] {tr("setup.license"), Web.MOD_LICENSE + tr("setup.mod.license")});
            facts.add(new String[] {tr("setup.to"), "mods/, config/mcef-modern/"});
        } else {
            facts.add(new String[] {tr("setup.size"), "~" + Web.NATIVES_MB + " MB"});
            facts.add(new String[] {tr("setup.from"), Web.NATIVES_SOURCE});
            facts.add(new String[] {tr("setup.license"), tr("setup.natives.license")});
            facts.add(new String[] {tr("setup.to"), "config/mcef-modern/"});
        }
        return facts;
    }

    private void pollBrowser() {
        if (need != SetupNeed.BROWSER_NATIVES || step != Step.DOWNLOAD) return;
        Web.State ws = Web.state(true);
        float p = Web.progress();
        progress = p < 0 ? 0 : p / 100f;
        if (ws == Web.State.READY) step = Step.DONE;
        if (ws == Web.State.FAILED) {
            error = Web.failure();
            step = Step.FAILED;
        }
    }

    private int drawStatus(Gfx g, int x, int y, int w) {
        switch (step) {
            case LOOKUP -> {
                g.text(tr("setup.lookup") + dots(), x, y, MUTED);
                return y + 14;
            }
            case DOWNLOAD -> {
                bar(g, x, y, w, 6, progress, null);
                String stage = need == SetupNeed.BROWSER_NATIVES ? Web.stage().toLowerCase(Locale.ROOT) : "";
                g.text(Math.round(progress * 100) + " %  " + stage, x, y + 10, MUTED);
                return y + 24;
            }
            case DONE -> {
                return lines(g, tr(need == SetupNeed.BROWSER_MOD ? "setup.done.restart" : "setup.done"), x, y, w, GREEN);
            }
            case FAILED -> {
                return lines(g, tr("setup.failed", error == null ? "?" : error), x, y, w, RED);
            }
            default -> {
                g.text(g.clip(tr("setup.nothing"), w), x, y, DIM);
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
            buttonRight(g, right, by, 18, tr("setup.close"), Style.PRIMARY, this::onClose);
            return;
        }
        boolean busy = step == Step.LOOKUP || step == Step.DOWNLOAD;
        String action = need == SetupNeed.BROWSER_NATIVES ? tr("setup.continue") : tr("setup.download");
        int left = buttonRight(g, right, by, 18, action, busy ? Style.DISABLED : Style.PRIMARY, this::go);
        buttonRight(g, left - 6, by, 18, tr("setup.later"), Style.SECONDARY, this::onClose);
    }

    public void go() {
        error = null;
        if (need == SetupNeed.BROWSER_MOD) {
            step = Step.LOOKUP;
            background(this::installMod);
        } else {
            Video.get().setBrowserConsent();
            Web.start();
            step = Step.DOWNLOAD;
        }
    }

    private void installMod() throws Exception {
        McefInstaller.Release rel = McefInstaller.lookup();
        step = Step.DOWNLOAD;
        McefInstaller.install(rel, f -> progress = f);
        Video.get().setBrowserConsent();
        step = Step.DONE;
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
        if (step == Step.DOWNLOAD && need != SetupNeed.BROWSER_NATIVES) return;
        Minecraft.getInstance().setScreen(parent);
    }
}
