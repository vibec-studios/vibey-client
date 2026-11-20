package dev.henny.vui.style;

import java.util.List;

import static dev.henny.vui.style.Prop.BORDER_BOTTOM;
import static dev.henny.vui.style.Prop.BORDER_BOTTOM_COLOR;
import static dev.henny.vui.style.Prop.BORDER_COLOR;
import static dev.henny.vui.style.Prop.BORDER_LEFT;
import static dev.henny.vui.style.Prop.BORDER_LEFT_COLOR;
import static dev.henny.vui.style.Prop.BORDER_RIGHT;
import static dev.henny.vui.style.Prop.BORDER_RIGHT_COLOR;
import static dev.henny.vui.style.Prop.BORDER_TOP;
import static dev.henny.vui.style.Prop.BORDER_TOP_COLOR;

final class BorderShorthands {
    static final List<Prop> WIDTHS = List.of(BORDER_TOP, BORDER_RIGHT, BORDER_BOTTOM, BORDER_LEFT);
    static final List<Prop> COLORS = List.of(BORDER_COLOR, BORDER_TOP_COLOR, BORDER_RIGHT_COLOR, BORDER_BOTTOM_COLOR,
            BORDER_LEFT_COLOR);

    private BorderShorthands() {
    }

    static List<Prop> longhandsOf(String name) {
        return switch (name) {
            case "border" -> List.of(BORDER_TOP, BORDER_RIGHT, BORDER_BOTTOM, BORDER_LEFT, BORDER_COLOR, BORDER_TOP_COLOR,
                    BORDER_RIGHT_COLOR, BORDER_BOTTOM_COLOR, BORDER_LEFT_COLOR);
            case "border-width" -> WIDTHS;
            case "border-color" -> COLORS;
            case "border-top" -> List.of(BORDER_TOP, BORDER_TOP_COLOR);
            case "border-right" -> List.of(BORDER_RIGHT, BORDER_RIGHT_COLOR);
            case "border-bottom" -> List.of(BORDER_BOTTOM, BORDER_BOTTOM_COLOR);
            case "border-left" -> List.of(BORDER_LEFT, BORDER_LEFT_COLOR);
            default -> null;
        };
    }

    static boolean expand(String name, List<String> w, List<Shorthands.Longhand> out) {
        switch (name) {
            case "border" -> side(out, w, WIDTHS, COLORS);
            case "border-color" -> colors(out, w);
            case "border-top" -> side(out, w, List.of(BORDER_TOP), List.of(BORDER_TOP_COLOR));
            case "border-right" -> side(out, w, List.of(BORDER_RIGHT), List.of(BORDER_RIGHT_COLOR));
            case "border-bottom" -> side(out, w, List.of(BORDER_BOTTOM), List.of(BORDER_BOTTOM_COLOR));
            case "border-left" -> side(out, w, List.of(BORDER_LEFT), List.of(BORDER_LEFT_COLOR));
            default -> {
                return false;
            }
        }
        return true;
    }

    private static void colors(List<Shorthands.Longhand> out, List<String> w) {
        if (w.isEmpty() || w.size() > 4) throw new StyleError("expected 1 to 4 colors", "e.g. border-color: $line or border-color: $brand $line");
        String t = w.get(0);
        String r = w.size() > 1 ? w.get(1) : t;
        String b = w.size() > 2 ? w.get(2) : t;
        String l = w.size() > 3 ? w.get(3) : r;
        out.add(new Shorthands.Longhand(BORDER_COLOR, t));
        out.add(new Shorthands.Longhand(BORDER_TOP_COLOR, t));
        out.add(new Shorthands.Longhand(BORDER_RIGHT_COLOR, r));
        out.add(new Shorthands.Longhand(BORDER_BOTTOM_COLOR, b));
        out.add(new Shorthands.Longhand(BORDER_LEFT_COLOR, l));
    }

    private static void side(List<Shorthands.Longhand> out, List<String> w, List<Prop> widths, List<Prop> colors) {
        String width = null;
        String color = null;
        for (String word : w) {
            if (ColorParser.looksLikeColor(word)) color = word;
            else if (word.equals("none")) width = "0";
            else if (!word.equals("solid")) width = word;
        }
        if (width == null && color == null) throw new StyleError("border needs a width and/or a color", "e.g. border: 1 $line");
        if (width == null) width = "1";
        for (Prop p : widths) out.add(new Shorthands.Longhand(p, width));
        if (color != null) for (Prop p : colors) out.add(new Shorthands.Longhand(p, color));
    }
}
