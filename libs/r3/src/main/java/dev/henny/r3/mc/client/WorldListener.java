package dev.henny.r3.mc.client;

public interface WorldListener {
    default void worldJoined(String worldKey) {}

    default void worldLeft(String worldKey) {}
}
