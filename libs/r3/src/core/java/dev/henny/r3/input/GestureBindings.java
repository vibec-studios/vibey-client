package dev.henny.r3.input;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GestureBindings {
    private final EnumMap<Gesture, Binding> bindings = new EnumMap<>(Gesture.class);

    public GestureBindings() {
        resetToDefaults();
    }

    public static GestureBindings defaults() {
        return new GestureBindings();
    }

    public GestureBindings resetToDefaults() {
        bindings.put(Gesture.USE, Binding.of(InputCode.MOUSE_LEFT));
        bindings.put(Gesture.GRAB, Binding.of(InputCode.MOUSE_MIDDLE));
        bindings.put(Gesture.MENU, Binding.of(InputCode.MOUSE_RIGHT));
        bindings.put(Gesture.CANCEL, Binding.of(InputCode.KEY_ESCAPE));
        bindings.put(Gesture.ROTATE, Binding.of(InputCode.KEY_R));
        bindings.put(Gesture.UNDO, Binding.with(InputCode.KEY_CONTROL, InputCode.KEY_Z));
        bindings.put(Gesture.RESIZE, Binding.of(InputCode.WHEEL));
        bindings.put(Gesture.DISTANCE_MODIFIER, Binding.of(InputCode.KEY_SHIFT));
        bindings.put(Gesture.FINE_MODIFIER, Binding.of(InputCode.KEY_ALT));
        return this;
    }

    public Binding get(Gesture gesture) {
        return bindings.get(gesture);
    }

    public GestureBindings set(Gesture gesture, Binding binding) {
        if (gesture == Gesture.RESIZE) return this;
        bindings.put(gesture, binding);
        return this;
    }

    public boolean pressed(Gesture gesture, InputCode code, HeldKeys held) {
        Binding b = bindings.get(gesture);
        return b != null && b.matches(code, held);
    }

    public boolean released(Gesture gesture, InputCode code) {
        Binding b = bindings.get(gesture);
        return b != null && b.code().equals(code);
    }

    public boolean held(Gesture gesture, HeldKeys keys) {
        Binding b = bindings.get(gesture);
        return b != null && b.held(keys);
    }

    public String displayName(Gesture gesture) {
        Binding b = bindings.get(gesture);
        return b == null ? "?" : b.displayName();
    }

    public Map<String, String> toMap() {
        Map<String, String> out = new LinkedHashMap<>();
        bindings.forEach((g, b) -> out.put(g.name(), (b.modifier() == null ? "" : b.modifier().id() + "+") + b.code().id()));
        return out;
    }

    public GestureBindings load(Map<String, String> map) {
        map.forEach((name, value) -> {
            Gesture g = parseGesture(name);
            Binding b = parseBinding(value);
            if (g != null && b != null) set(g, b);
        });
        return this;
    }

    private static Gesture parseGesture(String name) {
        try {
            return Gesture.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Binding parseBinding(String value) {
        if (value == null) return null;
        int plus = value.indexOf('+');
        InputCode code = InputCode.parse(plus < 0 ? value : value.substring(plus + 1));
        InputCode modifier = plus < 0 ? null : InputCode.parse(value.substring(0, plus));
        if (code == null || plus >= 0 && modifier == null) return null;
        return new Binding(code, modifier);
    }
}
