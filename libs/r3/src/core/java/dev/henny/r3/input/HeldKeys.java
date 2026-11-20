package dev.henny.r3.input;

@FunctionalInterface
public interface HeldKeys {
    HeldKeys NONE = code -> false;

    boolean isDown(InputCode code);
}
