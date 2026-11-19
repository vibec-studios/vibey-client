package dev.henny.r3.interact;

@FunctionalInterface
public interface InteractionListener {
    void onEvent(InteractionEvent event);
}
