package dev.henny.vibeyclient.ui.vui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vui.Vui;
import dev.henny.vui.expr.Functions;
import dev.henny.vui.expr.Translator;
import dev.henny.vui.mc.VuiMc;

public final class VibeyVui {
    private VibeyVui() {}

    private static boolean ready;

    public static Vui get() {
        if (!ready) init();
        return VuiMc.vui();
    }

    private static synchronized void init() {
        if (ready) return;
        ready = true;
        Lang.addFile("ui");
        Functions f = VuiMc.vui().functions();
        Translator before = f.translator();
        f.translator((key, args) -> Lang.has(key) ? Lang.tr(key, args) : before.translate(key, args));
        VuiMc.theme("vibeyclient:theme");
    }
}
