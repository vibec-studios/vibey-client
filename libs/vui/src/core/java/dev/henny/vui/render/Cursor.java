package dev.henny.vui.render;

public enum Cursor {
    DEFAULT, POINTER, TEXT, MOVE, GRAB, RESIZE_NS, RESIZE_EW, RESIZE_NWSE, RESIZE_NESW, NOT_ALLOWED;

    public static Cursor fromCss(String keyword) {
        if (keyword == null) return DEFAULT;
        return switch (keyword) {
            case "pointer" -> POINTER;
            case "text" -> TEXT;
            case "move" -> MOVE;
            case "grab" -> GRAB;
            case "resize-ns" -> RESIZE_NS;
            case "resize-ew" -> RESIZE_EW;
            case "resize-nwse" -> RESIZE_NWSE;
            case "resize-nesw" -> RESIZE_NESW;
            case "not-allowed" -> NOT_ALLOWED;
            default -> DEFAULT;
        };
    }
}
