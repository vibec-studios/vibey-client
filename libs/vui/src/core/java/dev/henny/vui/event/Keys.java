package dev.henny.vui.event;

public final class Keys {
    public static final int NONE = -1;
    public static final int SPACE = 32;
    public static final int A = 65;
    public static final int C = 67;
    public static final int V = 86;
    public static final int X = 88;
    public static final int Y = 89;
    public static final int Z = 90;
    public static final int ESCAPE = 256;
    public static final int ENTER = 257;
    public static final int TAB = 258;
    public static final int BACKSPACE = 259;
    public static final int INSERT = 260;
    public static final int DELETE = 261;
    public static final int RIGHT = 262;
    public static final int LEFT = 263;
    public static final int DOWN = 264;
    public static final int UP = 265;
    public static final int PAGE_UP = 266;
    public static final int PAGE_DOWN = 267;
    public static final int HOME = 268;
    public static final int END = 269;
    public static final int F1 = 290;
    public static final int KP_ENTER = 335;
    public static final int LEFT_SHIFT = 340;
    public static final int LEFT_CONTROL = 341;
    public static final int LEFT_ALT = 342;
    public static final int RIGHT_SHIFT = 344;
    public static final int RIGHT_CONTROL = 345;
    public static final int RIGHT_ALT = 346;
    public static final int MOUSE_BASE = -100;
    public static final int MOUSE_BUTTONS = 8;

    private Keys() {
    }

    public static int mouse(int button) {
        return MOUSE_BASE + button;
    }

    public static boolean isMouse(int code) {
        return code >= MOUSE_BASE && code < MOUSE_BASE + MOUSE_BUTTONS;
    }

    public static int mouseButton(int code) {
        return code - MOUSE_BASE;
    }

    public static String mouseName(int button) {
        return switch (button) {
            case 0 -> "Left Click";
            case 1 -> "Right Click";
            case 2 -> "Middle Click";
            default -> "Mouse " + (button + 1);
        };
    }

    public static boolean isEnter(int key) {
        return key == ENTER || key == KP_ENTER;
    }

    public static boolean isModifier(int key) {
        return key >= LEFT_SHIFT && key <= 348;
    }

    public static int byName(String name) {
        return switch (name) {
            case "enter" -> ENTER;
            case "esc", "escape" -> ESCAPE;
            case "tab" -> TAB;
            case "space" -> SPACE;
            case "up" -> UP;
            case "down" -> DOWN;
            case "left" -> LEFT;
            case "right" -> RIGHT;
            case "delete", "del" -> DELETE;
            case "backspace" -> BACKSPACE;
            case "home" -> HOME;
            case "end" -> END;
            case "pageup" -> PAGE_UP;
            case "pagedown" -> PAGE_DOWN;
            default -> name.length() == 1 && Character.isLetterOrDigit(name.charAt(0)) ? Character.toUpperCase(name.charAt(0)) : 0;
        };
    }

    public static String fallbackName(int key) {
        if (key == NONE) return "—";
        if (isMouse(key)) return mouseName(mouseButton(key));
        if (key >= 32 && key < 127) return String.valueOf((char) key);
        if (key >= F1 && key < F1 + 25) return "F" + (key - F1 + 1);
        return switch (key) {
            case ESCAPE -> "Esc";
            case ENTER -> "Enter";
            case TAB -> "Tab";
            case BACKSPACE -> "Backspace";
            case DELETE -> "Delete";
            case INSERT -> "Insert";
            case RIGHT -> "Right";
            case LEFT -> "Left";
            case DOWN -> "Down";
            case UP -> "Up";
            case HOME -> "Home";
            case END -> "End";
            case PAGE_UP -> "PgUp";
            case PAGE_DOWN -> "PgDn";
            case LEFT_SHIFT -> "LShift";
            case RIGHT_SHIFT -> "RShift";
            case LEFT_CONTROL -> "LCtrl";
            case RIGHT_CONTROL -> "RCtrl";
            case LEFT_ALT -> "LAlt";
            case RIGHT_ALT -> "RAlt";
            default -> "Key " + key;
        };
    }
}
