package dev.henny.vibeyclient.api;

import java.util.Locale;

/** Groups modules in the menu. Label: {@code core.category.<name in lower case>}. */
public enum ModuleCategory {
    CORE, SOCIAL, COSMETICS, MULTIPLAYER, MEDIA, CREATOR, MISC;

    public String nameKey() {
        return "core.category." + name().toLowerCase(Locale.ROOT);
    }
}
