package dev.henny.vibeyclient.module.waypoints.screen;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.waypoints.WaypointsRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class Screens {
    private Screens() {}

    public static ListScreen list(String selectId) {
        WaypointsRuntime rt = WaypointsRuntime.get();
        if (rt == null || !rt.active()) return null;
        ListScreen s = new ListScreen(new ListController(rt, selectId));
        s.parent(parent()).pause(false);
        s.open();
        return s;
    }

    public static WorldMapScreen map() {
        WaypointsRuntime rt = WaypointsRuntime.get();
        if (rt == null || !rt.active()) return null;
        WorldMapScreen s = new WorldMapScreen(rt);
        s.parent(parent()).background(false).pause(false);
        s.open();
        return s;
    }

    public static void closeCurrent() {
        Screen s = Minecraft.getInstance().screen;
        if (s != null) s.onClose();
    }

    private static Screen parent() {
        Screen s = ClientCompat.screen();
        return s instanceof ListScreen || s instanceof WorldMapScreen ? null : s;
    }
}
