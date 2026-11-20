package dev.henny.vibeyclient.module.clips.shots.core;

public record Shot(String name, long modifiedMs, long bytes) {
    public String key() {
        return name + "|" + modifiedMs + "|" + bytes;
    }
}
