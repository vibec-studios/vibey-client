package dev.henny.vibeyclient.api.service;

import java.util.List;

public interface WaypointPoints {
    record Point(String name, String dimension, int x, int y, int z) {}

    List<Point> points();
}
