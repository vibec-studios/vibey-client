package dev.henny.r3.input;

public record Binding(InputCode code, InputCode modifier) {
    public static Binding of(InputCode code) {
        return new Binding(code, null);
    }

    public static Binding with(InputCode modifier, InputCode code) {
        return new Binding(code, modifier);
    }

    public boolean matches(InputCode pressed, HeldKeys held) {
        return code.equals(pressed) && (modifier == null || held.isDown(modifier));
    }

    public boolean held(HeldKeys keys) {
        return keys.isDown(code) && (modifier == null || keys.isDown(modifier));
    }

    public String displayName() {
        return modifier == null ? code.displayName() : modifier.displayName() + "+" + code.displayName();
    }
}
