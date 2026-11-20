package dev.henny.vibeyclient.module.cosmetics;

import java.util.function.Consumer;

public final class EmoteHook {
    private EmoteHook() {}

    public static volatile Consumer<String> play = id -> {};
    public static volatile Runnable stop = () -> {};
}
