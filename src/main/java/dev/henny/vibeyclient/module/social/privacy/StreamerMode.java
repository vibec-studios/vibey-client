package dev.henny.vibeyclient.module.social.privacy;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.privacy.Privacy;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.module.social.SocialModule;

public final class StreamerMode {
    private static final String PART = "dev.henny.vibeyclient.module.social.streamer.StreamerPart";
    private static final int NAMES_EVERY_TICKS = 20;

    private final SocialModule module;
    private final boolean partPresent = present();
    private int ticks;

    public StreamerMode(SocialModule module) {
        this.module = module;
    }

    public boolean partPresent() {
        return partPresent;
    }

    public void toggle() {
        module.streamer.set(!module.streamer.on());
        boolean on = module.streamer.on();
        Notify.info(Lang.tr(on ? "social.streamer.on" : "social.streamer.off"), "");
    }

    public void apply() {
        boolean on = module.isEnabled() && module.streamer.on();
        Privacy.aliases().own(KnownNames.own(), Lang.tr("social.streamer.me"));
        Privacy.aliases().otherFormat(Lang.tr("social.streamer.player", "%d"));
        Privacy.set(on ? new Privacy.Flags(module.streamerNames.on(), module.streamerSkins.on(), module.streamerServer.on())
                : Privacy.Flags.OFF);
        if (on) Privacy.names(KnownNames.collect(module));
    }

    public void tick() {
        if (++ticks % NAMES_EVERY_TICKS != 0 || !Privacy.hideNames()) return;
        Privacy.names(KnownNames.collect(module));
    }

    private static boolean present() {
        try {
            Class.forName(PART, false, StreamerMode.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
