package dev.henny.vibeyclient.ui.vui.menu;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.VibeyModule;

import java.util.Set;

public final class ModuleLook {
    private ModuleLook() {}

    public static final Set<String> COLORED = Set.of("social", "cosmetics", "emotes", "multiplayer", "media", "devlink",
            "nametags", "hud", "pvp", "clips", "waypoints", "info", "camera", "visuals", "chat", "inventory", "servers",
            "cinematic", "screens", "companion");

    public static String icon(VibeyModule m) {
        String id = m.id().equals("lyrics") ? "media" : m.id();
        return COLORED.contains(id) ? "module_" + id : "puzzle";
    }

    public static String accent(VibeyModule m) {
        return String.format("#%06x", m.accentColor() & 0xFFFFFF);
    }

    public static String summary(VibeyModule m) {
        String k = m.id() + ".summary";
        return Lang.has(k) ? Lang.tr(k) : Lang.tr(m.descriptionKey());
    }

    public static boolean available(VibeyModule m) {
        return m.status().loadable() && m.canToggle();
    }

    public static String reason(VibeyModule m) {
        if (m.status() == ModuleManager.Status.CONFLICT) return Lang.tr("ui.module.installed");
        if (!m.status().loadable()) return Lang.tr("ui.module.unavailable");
        return "";
    }

    public static boolean blocked(VibeyModule m) {
        return !dev.henny.vibeyclient.ui.ServerBadge.disabledKeys(m).isEmpty();
    }
}
