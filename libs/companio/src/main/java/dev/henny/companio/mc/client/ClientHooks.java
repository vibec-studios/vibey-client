package dev.henny.companio.mc.client;

import dev.henny.companio.world.Companion;

import java.util.function.Consumer;

public final class ClientHooks {
    private Runnable menu = () -> {};
    private Consumer<Companion> tick = c -> {};

    public void menu(Runnable r) {
        menu = r;
    }

    public void tick(Consumer<Companion> c) {
        tick = c;
    }

    void openMenu() {
        menu.run();
    }

    void tick(Companion c) {
        tick.accept(c);
    }
}
