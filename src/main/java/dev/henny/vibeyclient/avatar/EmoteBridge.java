package dev.henny.vibeyclient.avatar;

import java.util.function.Consumer;

/** Local emote playback, provided by the emotes module (no-ops while it is off). */
public final class EmoteBridge {
    private EmoteBridge() {}

    public static volatile Consumer<String> play = id -> {};
    public static volatile Runnable stop = () -> {};
}
