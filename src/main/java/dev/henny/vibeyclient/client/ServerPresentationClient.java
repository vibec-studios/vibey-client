package dev.henny.vibeyclient.client;

import dev.henny.vibeyclient.net.ServerPresentation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ServerPresentationClient {
    private static List<ServerPresentation.Waypoint> waypoints = List.of();
    private static Map<UUID, Integer> colors = Map.of();

    private ServerPresentationClient() {}

    public static void replace(ServerPresentation value) {
        waypoints = value.waypoints();
        colors = value.resolvedColors();
    }

    public static List<ServerPresentation.Waypoint> waypoints() {
        return waypoints;
    }

    public static Integer nametagColor(UUID player) {
        return colors.get(player);
    }
}
