package dev.henny.vui.bind;

@FunctionalInterface
public interface Action {
    Object invoke(Object... args);
}
