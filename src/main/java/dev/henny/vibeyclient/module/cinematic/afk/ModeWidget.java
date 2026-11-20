package dev.henny.vibeyclient.module.cinematic.afk;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.cinematic.core.plan.Director;
import dev.henny.vibeyclient.module.cinematic.core.plan.Mode;
import dev.henny.vibeyclient.module.cinematic.run.Cinematic;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.TextWidget;

import java.util.Locale;

final class ModeWidget extends TextWidget {
    private Mode mode = Mode.MIX;

    @Override
    protected int rate() {
        return 4;
    }

    @Override
    protected void lines(HudPaint p) {
        mode = Cinematic.playing();
        Director d = Cinematic.director();
        String subject = d == null ? "" : d.subject();
        String name = name(mode);
        line(null, subject.isEmpty() ? name : name + " · " + subject);
    }

    static String name(Mode m) {
        return Lang.tr("cinematic.setting.mode." + m.name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected String icon(HudPaint p) {
        return switch (mode) {
            case BUILDS -> Icons.CUBE;
            case PLAYERS -> Icons.FRIENDS;
            case SELF -> Icons.PERSON;
            case PANORAMA, WAYPOINTS -> Icons.GLOBE;
            case SKY, DRONIE -> Icons.STAR;
            case MIX -> Icons.WAVE;
        };
    }
}
