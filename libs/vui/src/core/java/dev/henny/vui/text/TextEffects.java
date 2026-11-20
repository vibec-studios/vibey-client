package dev.henny.vui.text;

import dev.henny.vui.style.ColorParser;
import dev.henny.vui.style.CssValues;
import dev.henny.vui.style.StyleError;
import dev.henny.vui.text.effect.Gradient;
import dev.henny.vui.text.effect.Pulse;
import dev.henny.vui.text.effect.Rainbow;
import dev.henny.vui.text.effect.Shake;
import dev.henny.vui.text.effect.Shimmer;
import dev.henny.vui.text.effect.Typewriter;
import dev.henny.vui.text.effect.Wave;

import java.util.ArrayList;
import java.util.List;

public final class TextEffects {
    private TextEffects() {
    }

    public static TextEffect parse(String source) {
        String s = source.trim();
        if (s.isEmpty() || s.equals("none")) return null;
        List<TextEffect> effects = new ArrayList<>();
        for (String word : CssValues.words(s)) effects.add(one(word));
        return effects.size() == 1 ? effects.get(0) : new TextEffectChain(effects, s);
    }

    private static TextEffect one(String word) {
        int open = word.indexOf('(');
        String name = open < 0 ? word : word.substring(0, open);
        List<String> args = open < 0 ? List.of() : CssValues.arguments(word);
        return switch (name) {
            case "gradient" -> gradient(args);
            case "wave" -> new Wave(num(args, 0, 1f), num(args, 1, 1f), num(args, 2, 0.6f));
            case "rainbow" -> new Rainbow(num(args, 0, 0.5f), num(args, 1, 0.08f), num(args, 2, 0.6f));
            case "pulse" -> new Pulse(num(args, 0, 1f), num(args, 1, 0.4f));
            case "shimmer" -> new Shimmer(args.isEmpty() ? 0xFFFFFFFF : ColorParser.parse(args.get(0)), num(args, 1, 0.6f), num(args, 2, 12f));
            case "typewriter" -> new Typewriter(num(args, 0, 30f), num(args, 1, 0f));
            case "shake" -> new Shake(num(args, 0, 0.5f), num(args, 1, 20f));
            default -> throw new StyleError("unknown text effect '" + name + "'",
                    "known: gradient(c1, c2, …), wave(amp, speed), rainbow(speed), pulse(speed, min), shimmer(color, speed), typewriter(cps), shake(amp)");
        };
    }

    private static TextEffect gradient(List<String> args) {
        List<Integer> colors = new ArrayList<>();
        float speed = 0f;
        for (String a : args) {
            if (ColorParser.looksLikeColor(a)) colors.add(ColorParser.parse(a));
            else speed = parseNumber(a);
        }
        if (colors.isEmpty()) throw new StyleError("gradient() needs at least one color", "e.g. gradient(#8b6cff, #3fe0c5)");
        int[] array = new int[colors.size()];
        for (int i = 0; i < array.length; i++) array[i] = colors.get(i);
        return new Gradient(array, speed);
    }

    private static float num(List<String> args, int index, float fallback) {
        return index < args.size() ? parseNumber(args.get(index)) : fallback;
    }

    private static float parseNumber(String s) {
        try {
            return Float.parseFloat(s.trim());
        } catch (NumberFormatException e) {
            throw new StyleError("'" + s + "' is not a number", null);
        }
    }
}
