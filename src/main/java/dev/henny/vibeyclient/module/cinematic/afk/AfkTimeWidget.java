package dev.henny.vibeyclient.module.cinematic.afk;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.cinematic.run.Cinematic;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.TextWidget;

import java.util.Locale;

final class AfkTimeWidget extends TextWidget {
    @Override
    protected int rate() {
        return 2;
    }

    @Override
    protected void lines(HudPaint p) {
        long ms = Cinematic.active() && !Cinematic.preview() ? System.currentTimeMillis() - Cinematic.startedAt() : 260_000;
        line(Lang.tr("cinematic.afk.away"), format(ms));
    }

    static String format(long ms) {
        long s = Math.max(0, ms / 1000);
        long h = s / 3600, m = s / 60 % 60, sec = s % 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec) : String.format(Locale.ROOT, "%d:%02d", m, sec);
    }
}
