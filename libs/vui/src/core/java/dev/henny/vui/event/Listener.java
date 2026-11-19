package dev.henny.vui.event;

@FunctionalInterface
public interface Listener {
    void handle(Event event);
}
