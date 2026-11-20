package dev.henny.vui.style;

public final class StateFlags {
    public static final int HOVER = 1;
    public static final int FOCUS = 1 << 1;
    public static final int ACTIVE = 1 << 2;
    public static final int DISABLED = 1 << 3;
    public static final int CHECKED = 1 << 4;
    public static final int SELECTED = 1 << 5;
    public static final int OPEN = 1 << 6;
    public static final int ENTER = 1 << 7;
    public static final int LEAVE = 1 << 8;
    public static final int FIRST = 1 << 9;
    public static final int LAST = 1 << 10;
    public static final int FOCUS_WITHIN = 1 << 11;
    public static final int INVALID = 1 << 12;
    public static final int DRAGGING = 1 << 13;

    private StateFlags() {
    }

    public static int byName(String name) {
        return switch (name) {
            case "hover" -> HOVER;
            case "focus", "focus-visible" -> FOCUS;
            case "active", "pressed" -> ACTIVE;
            case "disabled" -> DISABLED;
            case "checked", "on" -> CHECKED;
            case "selected" -> SELECTED;
            case "open", "expanded" -> OPEN;
            case "enter" -> ENTER;
            case "leave" -> LEAVE;
            case "first-child", "first" -> FIRST;
            case "last-child", "last" -> LAST;
            case "focus-within" -> FOCUS_WITHIN;
            case "invalid" -> INVALID;
            case "dragging" -> DRAGGING;
            default -> 0;
        };
    }

    public static String names(int flags) {
        StringBuilder sb = new StringBuilder();
        String[] all = {"hover", "focus", "active", "disabled", "checked", "selected", "open", "enter", "leave",
                "first-child", "last-child", "focus-within", "invalid", "dragging"};
        for (int i = 0; i < all.length; i++) if ((flags & (1 << i)) != 0) sb.append(':').append(all[i]);
        return sb.toString();
    }
}
