package dev.henny.vibeyclient.ui.vui;

import dev.henny.vibeyclient.api.GlobalSettings;

public final class ClassicUi {
    private ClassicUi() {}

    public static final String PROPERTY = "vibeyclient.classicUi";

    public static boolean enabled() {
        String p = System.getProperty(PROPERTY);
        if (p != null) return Boolean.parseBoolean(p);
        return GlobalSettings.classicUi.on();
    }
}
