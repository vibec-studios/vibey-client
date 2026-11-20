package dev.henny.vui.mc;

import dev.henny.vui.Vui;
import dev.henny.vui.anim.Animations;
import dev.henny.vui.mc.dev.DevTools;
import dev.henny.vui.mc.host.HudHost;
import dev.henny.vui.mc.host.OverlayHost;
import dev.henny.vui.mc.host.ToastHost;
import dev.henny.vui.mc.host.VuiScreen;
import dev.henny.vui.mc.resource.Themes;
import dev.henny.vui.mc.texture.McTextures;
import dev.henny.vui.style.Theme;

public final class VuiMc {
    private VuiMc() {
    }

    public static Vui vui() {
        return VuiRuntime.get().vui();
    }

    public static VuiScreen screen(String documentId, Object controller) {
        return new VuiScreen(documentId, controller);
    }

    public static VuiScreen open(String documentId, Object controller) {
        return screen(documentId, controller).open();
    }

    public static HudHost hud() {
        return VuiRuntime.get().hud();
    }

    public static ToastHost toasts() {
        return VuiRuntime.get().toasts();
    }

    public static OverlayHost overlays() {
        return VuiRuntime.get().overlays();
    }

    public static McTextures textures() {
        return VuiRuntime.get().textures();
    }

    public static DevTools dev() {
        return VuiRuntime.get().dev();
    }

    public static void animations(boolean on) {
        Animations.setEnabled(on);
    }

    public static boolean theme(String themeId) {
        Theme theme = Themes.load(vui(), themeId);
        if (theme == null) return false;
        vui().theme(theme);
        vui().reloader().reloadAll();
        return true;
    }
}
