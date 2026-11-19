package dev.henny.r3.input;

import java.util.Objects;

public final class InputCode {
    public enum Device { MOUSE, KEY, WHEEL }

    public static final InputCode MOUSE_LEFT = new InputCode(Device.MOUSE, 0, "Left click");
    public static final InputCode MOUSE_RIGHT = new InputCode(Device.MOUSE, 1, "Right click");
    public static final InputCode MOUSE_MIDDLE = new InputCode(Device.MOUSE, 2, "Middle mouse");
    public static final InputCode WHEEL = new InputCode(Device.WHEEL, 0, "Wheel");
    public static final InputCode KEY_ESCAPE = new InputCode(Device.KEY, 256, "Esc");
    public static final InputCode KEY_SHIFT = new InputCode(Device.KEY, 340, "Shift");
    public static final InputCode KEY_CONTROL = new InputCode(Device.KEY, 341, "Ctrl");
    public static final InputCode KEY_ALT = new InputCode(Device.KEY, 342, "Alt");
    public static final InputCode KEY_R = new InputCode(Device.KEY, 82, "R");
    public static final InputCode KEY_Z = new InputCode(Device.KEY, 90, "Z");

    private static final InputCode[] KNOWN = {
            MOUSE_LEFT, MOUSE_RIGHT, MOUSE_MIDDLE, WHEEL, KEY_ESCAPE, KEY_SHIFT, KEY_CONTROL, KEY_ALT, KEY_R, KEY_Z
    };

    private final Device device;
    private final int code;
    private final String displayName;

    private InputCode(Device device, int code, String displayName) {
        this.device = device;
        this.code = code;
        this.displayName = displayName;
    }

    public static InputCode mouse(int button) {
        return lookup(Device.MOUSE, button, "Mouse " + (button + 1));
    }

    public static InputCode key(int keyCode) {
        String name = keyCode >= 32 && keyCode < 127 ? String.valueOf((char) keyCode) : "Key " + keyCode;
        return lookup(Device.KEY, keyCode, name);
    }

    private static InputCode lookup(Device device, int code, String fallbackName) {
        for (InputCode known : KNOWN) if (known.device == device && known.code == code) return known;
        return new InputCode(device, code, fallbackName);
    }

    public Device device() {
        return device;
    }

    public int code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public String id() {
        return device.name().toLowerCase(java.util.Locale.ROOT) + ":" + code;
    }

    public static InputCode parse(String id) {
        int colon = id == null ? -1 : id.indexOf(':');
        if (colon < 0) return null;
        try {
            int code = Integer.parseInt(id.substring(colon + 1));
            return switch (id.substring(0, colon)) {
                case "mouse" -> mouse(code);
                case "key" -> key(code);
                case "wheel" -> WHEEL;
                default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof InputCode other && other.device == device && other.code == code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(device, code);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
