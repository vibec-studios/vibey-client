package dev.henny.vibeyclient.ui.vui;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vui.Vui;
import dev.henny.vui.mc.VuiMc;

public final class VibeyVui {
    private VibeyVui() {}

    public static final String THEME = dev.henny.vibeyclient.ui.vui.theme.UiThemes.BASE;
    private static boolean ready;

    public static Vui vui() {
        init();
        return VuiMc.vui();
    }

    public static Vui get() {
        return vui();
    }

    public static synchronized void init() {
        if (ready) return;
        ready = true;
        Lang.addFile("ui");
        Vui vui = VuiMc.vui();
        vui.functions().translator(new LangTranslator());
        dev.henny.vibeyclient.ui.vui.theme.UiThemes.init(vui);
        VuiMc.animations(GlobalSettings.animate());
        GlobalSettings.animations.onChange(VuiMc::animations);
        Events.subscribe(VibeyEvents.LanguageChanged.class, e -> vui.reloader().reloadAll());
        DevControllers.register();
    }
}
