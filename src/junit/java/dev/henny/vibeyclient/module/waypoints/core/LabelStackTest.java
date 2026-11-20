package dev.henny.vibeyclient.module.waypoints.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LabelStackTest {
    private static LabelStack.Label at(String key, double yaw, double pitch, double distance) {
        return new LabelStack.Label(key, yaw, pitch, 0.1, 0.02, 1, distance);
    }

    @Test
    void hoveredLabelReservesItsExtraLines() {
        LabelStack.Label hovered = new LabelStack.Label("h", 0, 0, 0.1, 0.02, 3, 10);
        Map<String, Integer> l = LabelStack.levels(List.of(hovered, at("next", 0.01, 0.01, 20)));
        assertEquals(0, l.get("h"));
        assertEquals(3, l.get("next"));
    }

    @Test
    void nearestKeepsItsPlaceOthersStackUp() {
        Map<String, Integer> l = LabelStack.levels(List.of(at("far", 0.01, 0, 50), at("near", 0, 0, 10), at("mid", 0.02, 0, 30)));
        assertEquals(0, l.get("near"));
        assertEquals(1, l.get("mid"));
        assertEquals(2, l.get("far"));
    }

    @Test
    void apartLabelsStayFlat() {
        Map<String, Integer> l = LabelStack.levels(List.of(at("a", 0, 0, 10), at("b", 0.5, 0, 10), at("c", 0, 0.3, 12)));
        assertEquals(0, l.get("a"));
        assertEquals(0, l.get("b"));
        assertEquals(0, l.get("c"));
    }

    @Test
    void yawWrapsAround() {
        Map<String, Integer> l = LabelStack.levels(List.of(at("a", Math.PI - 0.01, 0, 10), at("b", -Math.PI + 0.01, 0, 20)));
        assertEquals(1, l.get("b"));
    }

    @Test
    void capsTheStack() {
        List<LabelStack.Label> many = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) many.add(at("w" + i, 0, 0, i + 1));
        assertEquals(LabelStack.MAX_LEVEL, LabelStack.levels(many).get("w9"));
    }
}
