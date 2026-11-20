package dev.henny.r3.support;

import dev.henny.r3.input.HeldKeys;
import dev.henny.r3.input.InputCode;

import java.util.HashSet;
import java.util.Set;

public final class TestKeys implements HeldKeys {
    private final Set<InputCode> down = new HashSet<>();

    public TestKeys hold(InputCode code) {
        down.add(code);
        return this;
    }

    public TestKeys release(InputCode code) {
        down.remove(code);
        return this;
    }

    @Override
    public boolean isDown(InputCode code) {
        return down.contains(code);
    }
}
