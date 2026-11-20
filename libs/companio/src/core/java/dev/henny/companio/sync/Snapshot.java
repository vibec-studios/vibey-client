package dev.henny.companio.sync;

import java.util.List;

public record Snapshot(String pack, String hash, String companion, String variant, String name, float size,
                       double x, double y, double z, float yaw, String dimension, String base, List<String> layers,
                       List<String> props, String bubble, boolean held) {
    public String key() {
        return pack + ":" + companion;
    }
}
