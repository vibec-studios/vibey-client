package dev.henny.vibeyclient.ui.vui;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vui.Vui;
import dev.henny.vui.mc.VuiMc;

public final class VibeyVui {
    private VibeyVui() {}

    public static final String THEME = "assets/vibeyclient/vui/theme.json";
    private static boolean ready;

    public static Vui vui() {
        init();
        return VuiMc.vui();
    }

    public static synchronized void init() {
        if (ready) return;
        ready = true;
        Lang.addFile("ui");
        Vui vui = VuiMc.vui();
        vui.functions().translator(new LangTranslator());
        vui.loadTheme(THEME);
        if (!vui.themeDiagnostics().isEmpty()) Vibey.LOG.warn("vui theme: {}", vui.themeDiagnostics().format());
        VuiMc.animations(GlobalSettings.animate());
        GlobalSettings.animations.onChange(VuiMc::animations);
        Events.subscribe(VibeyEvents.LanguageChanged.class, e -> vui.reloader().reloadAll());
        DevControllers.register();
    }
}
