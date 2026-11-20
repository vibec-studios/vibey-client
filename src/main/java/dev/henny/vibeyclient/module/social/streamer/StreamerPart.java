package dev.henny.vibeyclient.module.social.streamer;

import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.event.VibeyEvents;

public final class StreamerPart {
    private StreamerPart() {}

    public static void install() {
        Events.subscribe(VibeyEvents.ScreenInit.class, e -> AddressMask.apply(e.screen()));
    }
}
