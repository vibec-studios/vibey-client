package dev.henny.vibeyclient.ui.vui.menu;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.VibeyModule;

public final class ModuleLook {
    private ModuleLook() {}

    public static String icon(VibeyModule m) {
        return switch (m.id()) {
            case "social" -> "friends";
            case "cosmetics" -> "shirt";
            case "emotes" -> "wave";
            case "multiplayer" -> "host";
            case "lyrics", "media" -> "note";
            case "devlink" -> "cube";
            case "nametags" -> "seal";
            case "hud" -> "hud";
            case "pvp" -> "crosshair";
            case "clips" -> "play";
            case "waypoints" -> "pin";
            case "info" -> "info";
            case "camera" -> "camera";
            case "visuals" -> "palette";
            case "chat" -> "chat";
            case "inventory" -> "bag";
            case "servers" -> "server";
            case "cinematic" -> "film";
            case "screens" -> "window";
            case "companion" -> "paw";
            default -> "puzzle";
        };
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
