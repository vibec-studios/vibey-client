package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.V3;
import dev.henny.vibeyclient.module.cinematic.core.WorldView;

import java.util.List;
import java.util.function.Supplier;

public record Scene(WorldView world, V3 eye, V3 feet, double facing, double radius, double speed,
                    List<Subject> players, List<Mark> marks, Sky sky) {

    public record Subject(String id, String name, Supplier<V3> feet, double height) {
        public V3 head() {
            return feet.get().add(0, height * 0.9, 0);
        }
    }

    public record Mark(String name, V3 pos) {}

    public record Sky(double sunYaw, boolean sunLow, boolean night) {
        public static final Sky NOON = new Sky(-90, false, false);
    }

    public double time(double seconds) {
        return seconds / Math.max(0.25, speed);
    }

    public double frontAngle() {
        return -facing;
    }
}
