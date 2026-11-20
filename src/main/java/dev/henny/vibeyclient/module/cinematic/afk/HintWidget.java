package dev.henny.vibeyclient.module.cinematic.afk;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.TextWidget;

final class HintWidget extends TextWidget {
    @Override
    protected int rate() {
        return 1;
    }

    @Override
    protected void lines(HudPaint p) {
        line(null, Lang.tr("cinematic.afk.hint"));
    }
}
