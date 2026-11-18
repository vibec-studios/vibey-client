package dev.henny.vibeyclient.module.media.lyrics;

import dev.henny.vibeyclient.api.Lang;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Short access to this module's i18n file ({@code i18n/<lang>/media.lyrics.json}); keys without the "media.lyrics." prefix. */
public final class T {
    private static final String PREFIX = "media.lyrics.";

    private T() {}

    public static String get(String key, Object... args) {
        return Lang.tr(PREFIX + key, args);
    }

    public static MutableComponent text(String key, Object... args) {
        return Component.literal(get(key, args));
    }

    public static boolean has(String key) {
        return Lang.has(PREFIX + key);
    }
}
