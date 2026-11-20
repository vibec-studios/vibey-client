package dev.henny.vui.style;

import java.util.HashMap;
import java.util.Map;

import static dev.henny.vui.style.PropKind.KEYWORD;
import static dev.henny.vui.style.PropKind.LENGTH;
import static dev.henny.vui.style.PropKind.NUMBER;
import static dev.henny.vui.style.PropKind.OBJECT;
import static dev.henny.vui.style.PropKind.STRING;

public enum Prop {
    DISPLAY("display", KEYWORD, false, true, "column", "column", "row", "stack", "grid", "none"),
    WIDTH("width", LENGTH, false, true, "auto"),
    HEIGHT("height", LENGTH, false, true, "auto"),
    MIN_WIDTH("min-width", LENGTH, false, true, "0"),
    MIN_HEIGHT("min-height", LENGTH, false, true, "0"),
    MAX_WIDTH("max-width", LENGTH, false, true, "none"),
    MAX_HEIGHT("max-height", LENGTH, false, true, "none"),
    GROW("flex-grow", NUMBER, false, true, "0"),
    SHRINK("flex-shrink", NUMBER, false, true, "1"),
    BASIS("flex-basis", LENGTH, false, true, "auto"),
    ROW_GAP("row-gap", LENGTH, false, true, "0"),
    COLUMN_GAP("column-gap", LENGTH, false, true, "0"),
    PADDING_TOP("padding-top", LENGTH, false, true, "0"),
    PADDING_RIGHT("padding-right", LENGTH, false, true, "0"),
    PADDING_BOTTOM("padding-bottom", LENGTH, false, true, "0"),
    PADDING_LEFT("padding-left", LENGTH, false, true, "0"),
    MARGIN_TOP("margin-top", LENGTH, false, true, "0"),
    MARGIN_RIGHT("margin-right", LENGTH, false, true, "0"),
    MARGIN_BOTTOM("margin-bottom", LENGTH, false, true, "0"),
    MARGIN_LEFT("margin-left", LENGTH, false, true, "0"),
    ALIGN_ITEMS("align-items", KEYWORD, false, true, "stretch", "start", "center", "end", "stretch"),
    ALIGN_SELF("align-self", KEYWORD, false, true, "auto", "auto", "start", "center", "end", "stretch"),
    JUSTIFY("justify-content", KEYWORD, false, true, "start",
            "start", "center", "end", "space-between", "space-around", "space-evenly"),
    WRAP("flex-wrap", KEYWORD, false, true, "nowrap", "nowrap", "wrap"),
    POSITION("position", KEYWORD, false, true, "relative", "relative", "absolute", "fixed"),
    ANCHOR("anchor", KEYWORD, false, true, "none", "none", "top-left", "top", "top-right", "left", "center",
            "right", "bottom-left", "bottom", "bottom-right"),
    TOP("top", LENGTH, false, true, "auto"),
    RIGHT("right", LENGTH, false, true, "auto"),
    BOTTOM("bottom", LENGTH, false, true, "auto"),
    LEFT("left", LENGTH, false, true, "auto"),
    OVERFLOW("overflow", KEYWORD, false, true, "visible", "visible", "hidden", "scroll"),
    GRID_COLUMNS("grid-columns", NUMBER, false, true, "0"),
    GRID_MIN_COLUMN("grid-min-column", LENGTH, false, true, "0"),
    GRID_COLUMN_SPAN("grid-column-span", NUMBER, false, true, "1"),
    GRID_ROW_SPAN("grid-row-span", NUMBER, false, true, "1"),
    ASPECT_RATIO("aspect-ratio", NUMBER, false, true, "0"),
    BORDER_TOP("border-top-width", LENGTH, false, true, "0"),
    BORDER_RIGHT("border-right-width", LENGTH, false, true, "0"),
    BORDER_BOTTOM("border-bottom-width", LENGTH, false, true, "0"),
    BORDER_LEFT("border-left-width", LENGTH, false, true, "0"),
    BORDER_COLOR("border-color", PropKind.COLOR, false, false, "transparent"),
    BORDER_TOP_COLOR("border-top-color", PropKind.COLOR, false, false, "transparent"),
    BORDER_RIGHT_COLOR("border-right-color", PropKind.COLOR, false, false, "transparent"),
    BORDER_BOTTOM_COLOR("border-bottom-color", PropKind.COLOR, false, false, "transparent"),
    BORDER_LEFT_COLOR("border-left-color", PropKind.COLOR, false, false, "transparent"),
    BACKGROUND("background-color", PropKind.COLOR, false, false, "transparent"),
    GRADIENT_TO("gradient-to", PropKind.COLOR, false, false, "transparent"),
    GRADIENT_DIRECTION("gradient-direction", KEYWORD, false, false, "vertical", "vertical", "horizontal"),
    BACKGROUND_IMAGE("background-image", STRING, false, false, "none"),
    BACKGROUND_SLICE("background-slice", NUMBER, false, false, "0"),
    COLOR("color", PropKind.COLOR, true, false, "#ffffffff"),
    ACCENT("accent-color", PropKind.COLOR, true, false, "#8b6cffff"),
    TRACK_COLOR("track-color", PropKind.COLOR, false, false, "#18181fff"),
    THUMB_COLOR("thumb-color", PropKind.COLOR, false, false, "#f4f4f5ff"),
    OPACITY("opacity", NUMBER, false, false, "1"),
    FONT_SCALE("font-scale", NUMBER, true, true, "1"),
    FONT_WEIGHT("font-weight", KEYWORD, true, true, "normal", "normal", "bold"),
    TEXT_ALIGN("text-align", KEYWORD, true, true, "start", "start", "center", "end"),
    TEXT_WRAP("text-wrap", KEYWORD, true, true, "wrap", "wrap", "nowrap"),
    TEXT_OVERFLOW("text-overflow", KEYWORD, true, true, "clip", "clip", "ellipsis"),
    TEXT_TRANSFORM("text-transform", KEYWORD, true, true, "none", "none", "uppercase", "lowercase"),
    TEXT_SHADOW("text-shadow", KEYWORD, true, false, "none", "none", "on"),
    TEXT_EFFECT("text-effect", OBJECT, true, false, "none"),
    MAX_LINES("max-lines", NUMBER, true, true, "0"),
    LINE_HEIGHT("line-height", LENGTH, true, true, "auto"),
    LETTER_SPACING("letter-spacing", LENGTH, true, true, "0"),
    TRANSLATE_X("translate-x", LENGTH, false, false, "0"),
    TRANSLATE_Y("translate-y", LENGTH, false, false, "0"),
    SCALE("scale", NUMBER, false, false, "1"),
    ROTATE("rotate", NUMBER, false, false, "0"),
    TRANSITION("transition", OBJECT, false, false, "none"),
    ANIMATION("animation", OBJECT, false, false, "none"),
    CURSOR("cursor", KEYWORD, true, false, "default",
            "default", "pointer", "text", "move", "grab", "resize-ns", "resize-ew", "resize-nwse", "resize-nesw", "not-allowed"),
    VISIBILITY("visibility", KEYWORD, true, false, "visible", "visible", "hidden"),
    POINTER_EVENTS("pointer-events", KEYWORD, false, false, "auto", "auto", "none");

    private static final Map<String, Prop> BY_NAME = new HashMap<>();

    static {
        for (Prop p : values()) BY_NAME.put(p.cssName, p);
    }

    private final String cssName;
    private final PropKind kind;
    private final boolean inherited;
    private final boolean affectsLayout;
    private final String initial;
    private final String[] keywords;

    Prop(String cssName, PropKind kind, boolean inherited, boolean affectsLayout, String initial, String... keywords) {
        this.cssName = cssName;
        this.kind = kind;
        this.inherited = inherited;
        this.affectsLayout = affectsLayout;
        this.initial = initial;
        this.keywords = keywords;
    }

    public static Prop byName(String name) {
        return BY_NAME.get(name);
    }

    public String cssName() {
        return cssName;
    }

    public PropKind kind() {
        return kind;
    }

    public boolean inherited() {
        return inherited;
    }

    public boolean affectsLayout() {
        return affectsLayout;
    }

    public String initial() {
        return initial;
    }

    public String[] keywords() {
        return keywords;
    }

    public boolean allows(String keyword) {
        if (keywords.length == 0) return true;
        for (String k : keywords) if (k.equals(keyword)) return true;
        return false;
    }
}
