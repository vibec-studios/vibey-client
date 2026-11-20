package dev.henny.vibeyclient.module.cinematic.afk;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.SocialStatus;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.TextWidget;

final class FriendsWidget extends TextWidget {
    @Override
    protected int rate() {
        return 1;
    }

    @Override
    protected void lines(HudPaint p) {
        int n = Services.get(SocialStatus.class).map(SocialStatus::onlineFriends).orElse(-1);
        if (n < 0 && p.preview) n = 3;
        if (n >= 0) line(Lang.tr("cinematic.afk.friends"), Integer.toString(n));
    }

    @Override
    protected String icon(HudPaint p) {
        return Icons.FRIENDS;
    }
}
