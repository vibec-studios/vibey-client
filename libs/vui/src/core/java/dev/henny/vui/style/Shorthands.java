package dev.henny.vui.style;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static dev.henny.vui.style.Prop.BORDER_BOTTOM;
import static dev.henny.vui.style.Prop.BORDER_LEFT;
import static dev.henny.vui.style.Prop.BORDER_RIGHT;
import static dev.henny.vui.style.Prop.BORDER_TOP;

public final class Shorthands {
    private static final Set<String> NAMES = Set.of("padding", "margin", "gap", "border", "border-top", "border-right",
            "border-bottom", "border-left", "border-width", "inset", "background", "flex", "size", "gradient", "padding-x",
            "padding-y", "margin-x", "margin-y", "translate");

    private Shorthands() {
    }

    public record Longhand(Prop prop, String value) {
    }

    public static Set<String> names() {
        return NAMES;
    }

    public static boolean isShorthand(String name) {
        return NAMES.contains(name);
    }

    public static List<Prop> longhandsOf(String name) {
        Prop single = Prop.byName(name);
        if (single != null) return List.of(single);
        return switch (name) {
            case "padding" -> List.of(Prop.PADDING_TOP, Prop.PADDING_RIGHT, Prop.PADDING_BOTTOM, Prop.PADDING_LEFT);
            case "margin" -> List.of(Prop.MARGIN_TOP, Prop.MARGIN_RIGHT, Prop.MARGIN_BOTTOM, Prop.MARGIN_LEFT);
            case "gap" -> List.of(Prop.ROW_GAP, Prop.COLUMN_GAP);
            case "border" -> List.of(BORDER_TOP, BORDER_RIGHT, BORDER_BOTTOM, BORDER_LEFT, Prop.BORDER_COLOR);
            case "border-width" -> List.of(BORDER_TOP, BORDER_RIGHT, BORDER_BOTTOM, BORDER_LEFT);
            case "inset" -> List.of(Prop.TOP, Prop.RIGHT, Prop.BOTTOM, Prop.LEFT);
            case "background" -> List.of(Prop.BACKGROUND, Prop.GRADIENT_TO);
            case "size" -> List.of(Prop.WIDTH, Prop.HEIGHT);
            case "translate" -> List.of(Prop.TRANSLATE_X, Prop.TRANSLATE_Y);
            default -> List.of();
        };
    }

    public static List<Longhand> expand(String name, String value) {
        List<Longhand> out = new ArrayList<>();
        List<String> w = CssValues.words(value);
        switch (name) {
            case "padding" -> box(out, w, Prop.PADDING_TOP, Prop.PADDING_RIGHT, Prop.PADDING_BOTTOM, Prop.PADDING_LEFT);
            case "margin" -> box(out, w, Prop.MARGIN_TOP, Prop.MARGIN_RIGHT, Prop.MARGIN_BOTTOM, Prop.MARGIN_LEFT);
            case "inset" -> box(out, w, Prop.TOP, Prop.RIGHT, Prop.BOTTOM, Prop.LEFT);
            case "border-width" -> box(out, w, BORDER_TOP, BORDER_RIGHT, BORDER_BOTTOM, BORDER_LEFT);
            case "padding-x" -> pair(out, w, Prop.PADDING_LEFT, Prop.PADDING_RIGHT);
            case "padding-y" -> pair(out, w, Prop.PADDING_TOP, Prop.PADDING_BOTTOM);
            case "margin-x" -> pair(out, w, Prop.MARGIN_LEFT, Prop.MARGIN_RIGHT);
            case "margin-y" -> pair(out, w, Prop.MARGIN_TOP, Prop.MARGIN_BOTTOM);
            case "gap" -> two(out, w, Prop.ROW_GAP, Prop.COLUMN_GAP);
            case "size" -> two(out, w, Prop.WIDTH, Prop.HEIGHT);
            case "translate" -> two(out, w, Prop.TRANSLATE_X, Prop.TRANSLATE_Y);
            case "border" -> border(out, w, BORDER_TOP, BORDER_RIGHT, BORDER_BOTTOM, BORDER_LEFT);
            case "border-top" -> border(out, w, BORDER_TOP);
            case "border-right" -> border(out, w, BORDER_RIGHT);
            case "border-bottom" -> border(out, w, BORDER_BOTTOM);
            case "border-left" -> border(out, w, BORDER_LEFT);
            case "background", "gradient" -> background(out, w);
            case "flex" -> flex(out, w);
            default -> throw new StyleError("unknown shorthand '" + name + "'", null);
        }
        return out;
    }

    private static void box(List<Longhand> out, List<String> w, Prop top, Prop right, Prop bottom, Prop left) {
        if (w.isEmpty() || w.size() > 4) throw new StyleError("expected 1 to 4 values", "e.g. padding: 4 8");
        String t = w.get(0);
        String r = w.size() > 1 ? w.get(1) : t;
        String b = w.size() > 2 ? w.get(2) : t;
        String l = w.size() > 3 ? w.get(3) : r;
        out.add(new Longhand(top, t));
        out.add(new Longhand(right, r));
        out.add(new Longhand(bottom, b));
        out.add(new Longhand(left, l));
    }

    private static void pair(List<Longhand> out, List<String> w, Prop a, Prop b) {
        if (w.size() != 1) throw new StyleError("expected one value", null);
        out.add(new Longhand(a, w.get(0)));
        out.add(new Longhand(b, w.get(0)));
    }

    private static void two(List<Longhand> out, List<String> w, Prop first, Prop second) {
        if (w.isEmpty() || w.size() > 2) throw new StyleError("expected 1 or 2 values", "e.g. gap: 4 8");
        out.add(new Longhand(first, w.get(0)));
        out.add(new Longhand(second, w.size() > 1 ? w.get(1) : w.get(0)));
    }

    private static void border(List<Longhand> out, List<String> w, Prop... sides) {
        String width = null;
        String color = null;
        for (String word : w) {
            if (ColorParser.looksLikeColor(word)) color = word;
            else if (word.equals("none")) width = "0";
            else if (!word.equals("solid")) width = word;
        }
        if (width == null && color == null) throw new StyleError("border needs a width and/or a color", "e.g. border: 1 $line");
        if (width == null) width = "1";
        for (Prop side : sides) out.add(new Longhand(side, width));
        if (color != null) out.add(new Longhand(Prop.BORDER_COLOR, color));
    }

    private static void background(List<Longhand> out, List<String> w) {
        List<String> colors = new ArrayList<>();
        String direction = null;
        String image = null;
        for (String word : w) {
            if (word.equals("vertical") || word.equals("horizontal")) direction = word;
            else if (word.startsWith("url(")) image = word;
            else colors.add(word);
        }
        if (image != null) out.add(new Longhand(Prop.BACKGROUND_IMAGE, image));
        if (colors.size() > 2) throw new StyleError("background takes one color or two for a gradient", "e.g. background: $panel or background: #000 #222 vertical");
        if (!colors.isEmpty()) out.add(new Longhand(Prop.BACKGROUND, colors.get(0)));
        out.add(new Longhand(Prop.GRADIENT_TO, colors.size() == 2 ? colors.get(1) : "transparent"));
        if (direction != null) out.add(new Longhand(Prop.GRADIENT_DIRECTION, direction));
    }

    private static void flex(List<Longhand> out, List<String> w) {
        if (w.size() == 1 && w.get(0).equals("none")) {
            out.add(new Longhand(Prop.GROW, "0"));
            out.add(new Longhand(Prop.SHRINK, "0"));
            return;
        }
        if (w.isEmpty() || w.size() > 3) throw new StyleError("flex takes grow [shrink] [basis]", "e.g. flex: 1");
        out.add(new Longhand(Prop.GROW, w.get(0)));
        out.add(new Longhand(Prop.SHRINK, w.size() > 1 ? w.get(1) : "1"));
        out.add(new Longhand(Prop.BASIS, w.size() > 2 ? w.get(2) : "0"));
    }
}
