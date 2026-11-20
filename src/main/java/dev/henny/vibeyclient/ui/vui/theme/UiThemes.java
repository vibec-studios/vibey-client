package dev.henny.vibeyclient.ui.vui.theme;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.GlobalSettings.UiTheme;
import dev.henny.vui.Vui;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class UiThemes {
    public static final String BASE = "assets/vibeyclient/vui/theme.json";

    private static Vui vui;

    private UiThemes() {}

    public static void init(Vui target) {
        vui = target;
        GlobalSettings.theme.onChange(t -> apply());
        GlobalSettings.ownAccent.onChange(on -> apply());
        GlobalSettings.accent.onChange(c -> apply());
        apply();
    }

    public static List<String> files(UiTheme theme) {
        if (theme == UiTheme.DARK) return List.of(BASE);
        return List.of(BASE, "assets/vibeyclient/vui/theme." + theme.name().toLowerCase(Locale.ROOT) + ".json");
    }

    public static void apply() {
        if (vui == null) return;
        List<String> files = files(GlobalSettings.theme.get());
        vui.useThemes(files, Map.of());
        if (GlobalSettings.ownAccent.on()) vui.useThemes(files, Accent.tokens(GlobalSettings.accent.argb(), vui.theme().get("surface3")));
        if (!vui.themeDiagnostics().isEmpty()) Vibey.LOG.warn("vui theme: {}", vui.themeDiagnostics().format());
        vui.reloader().reloadAll();
    }

    public static Map<String, String> active() {
        return vui == null ? Map.of() : vui.theme().tokens();
    }
}
