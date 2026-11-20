package dev.henny.vibeyclient.module.media.video;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.media.ffmpeg.Ffmpeg;
import dev.henny.vibeyclient.media.player.MediaPlayer;
import dev.henny.vibeyclient.module.media.video.ui.SetupNeed;
import dev.henny.vibeyclient.module.media.video.web.McefInstaller;
import dev.henny.vibeyclient.module.media.video.web.Web;

import static dev.henny.vibeyclient.module.media.video.Video.tr;

public final class ScreenStatus {
    private final Video video;

    ScreenStatus(Video video) {
        this.video = video;
    }

    public boolean browserConsent() {
        JsonObject d = video.cfg.data();
        return d.has("browserConsent") && d.get("browserConsent").getAsBoolean();
    }

    public void setBrowserConsent() {
        video.cfg.data().addProperty("browserConsent", true);
        video.cfg.markDirty();
    }

    public String line(MediaScreen s) {
        if (s.source == null) return tr("status.nosource");
        MediaPlayer p = s.player;
        if (p != null && p.status() != MediaPlayer.Status.ERROR && p.texture() != null) {
            return p.audioOnly() ? "♪ " + s.source.label() : null;
        }
        if (s.remote && !video.players.allowed(s)) {
            return video.cfg.others.get() == VideoFeature.Others.NEVER ? tr("status.hidden") : tr("status.ask", s.ownerName, s.source.label());
        }
        String setup = s.source.kind().web() ? browserLine() : Ffmpeg.available() ? null : tr("status.needffmpeg");
        if (setup != null) return setup;
        if (p == null) return s.placed() && s.distance > video.cfg.distance.get() ? tr("status.far") : tr("status.loading");
        if (p.status() == MediaPlayer.Status.ERROR) return tr("status.error", p.error() == null ? "" : p.error());
        if (p.audioOnly()) return "♪ " + s.source.label();
        return p.texture() == null ? tr("status.loading") : null;
    }

    private String browserLine() {
        return switch (Web.state(browserConsent())) {
            case NOT_INSTALLED, NEEDS_SETUP -> McefInstaller.installedThisSession ? tr("status.restart") : tr("status.needbrowser");
            case FAILED -> tr("status.browserfailed");
            case STARTING -> {
                float p = Web.progress();
                boolean downloading = p >= 0 && p <= 100 && !Web.stage().isEmpty() && !Web.stage().equals("INITIALIZING");
                yield downloading ? tr("status.browserdownload", Math.round(p)) : tr("status.browserstart");
            }
            default -> null;
        };
    }

    public SetupNeed missing(MediaScreen s) {
        if (s.source == null) return null;
        if (!s.source.kind().web()) return Ffmpeg.available() ? null : SetupNeed.FFMPEG;
        Web.State st = Web.state(browserConsent());
        if (st == Web.State.NOT_INSTALLED) return McefInstaller.installedThisSession ? null : SetupNeed.BROWSER_MOD;
        return st == Web.State.NEEDS_SETUP ? SetupNeed.BROWSER_NATIVES : null;
    }

    public boolean needsAccept(MediaScreen s) {
        return s.remote && !s.accepted && video.cfg.others.get() == VideoFeature.Others.ASK;
    }
}
