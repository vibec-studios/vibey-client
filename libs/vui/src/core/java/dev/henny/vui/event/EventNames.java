package dev.henny.vui.event;

import java.util.Set;

public final class EventNames {
    public static final String POINTER_MOVE = "pointermove";
    public static final String POINTER_DOWN = "pointerdown";
    public static final String POINTER_UP = "pointerup";
    public static final String POINTER_ENTER = "pointerenter";
    public static final String POINTER_LEAVE = "pointerleave";
    public static final String CLICK = "click";
    public static final String DOUBLE_CLICK = "dblclick";
    public static final String CONTEXT_MENU = "contextmenu";
    public static final String DRAG_START = "dragstart";
    public static final String DRAG = "drag";
    public static final String DRAG_END = "dragend";
    public static final String WHEEL = "wheel";
    public static final String KEY_DOWN = "keydown";
    public static final String KEY_UP = "keyup";
    public static final String CHAR = "char";
    public static final String FOCUS = "focus";
    public static final String BLUR = "blur";
    public static final String CHANGE = "change";
    public static final String INPUT = "input";
    public static final String SUBMIT = "submit";
    public static final String CLOSE = "close";
    public static final String ESCAPE = "escape";

    private static final Set<String> NON_BUBBLING = Set.of(POINTER_ENTER, POINTER_LEAVE, FOCUS, BLUR);

    private EventNames() {
    }

    public static String canonical(String name) {
        return switch (name) {
            case "mousemove", "move", "hover" -> POINTER_MOVE;
            case "mousedown", "press", "down" -> POINTER_DOWN;
            case "mouseup", "release", "up" -> POINTER_UP;
            case "mouseenter", "enter" -> POINTER_ENTER;
            case "mouseleave", "leave" -> POINTER_LEAVE;
            case "doubleclick", "double" -> DOUBLE_CLICK;
            case "rightclick" -> CONTEXT_MENU;
            case "scroll", "mousewheel" -> WHEEL;
            case "key", "keypress" -> KEY_DOWN;
            case "type", "text" -> CHAR;
            default -> name;
        };
    }

    public static boolean bubbles(String type) {
        return !NON_BUBBLING.contains(type);
    }

    public static boolean isPointer(String type) {
        return type.startsWith("pointer") || type.equals(CLICK) || type.equals(DOUBLE_CLICK) || type.equals(CONTEXT_MENU)
                || type.startsWith("drag") || type.equals(WHEEL);
    }
}
