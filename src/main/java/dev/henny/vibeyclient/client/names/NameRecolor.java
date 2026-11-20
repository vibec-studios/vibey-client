package dev.henny.vibeyclient.client.names;

import dev.henny.vibeyclient.ranks.style.NameStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class NameRecolor {
    private NameRecolor() {}

    private record Part(Style style, String text) {}

    public static Component apply(Component name, String profileName, NameStyle style, double t) {
        if (name == null || style == null || profileName == null || profileName.isEmpty()) return name;
        List<Part> parts = parts(name);
        int target = find(parts, profileName);
        if (target < 0) return name;
        MutableComponent out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            Part p = parts.get(i);
            if (i == target) styled(out, p, profileName, style, t);
            else out.append(Component.literal(p.text()).setStyle(p.style()));
        }
        return out;
    }

    public static MutableComponent colored(String text, Style base, NameStyle style, double t) {
        MutableComponent out = Component.empty();
        int[] cps = text.codePoints().toArray();
        for (int i = 0; i < cps.length; i++) {
            int rgb = style.colorAt(i, cps.length, t);
            out.append(Component.literal(new String(cps, i, 1)).setStyle(base.withColor(rgb)));
        }
        return out;
    }

    private static void styled(MutableComponent out, Part p, String profileName, NameStyle style, double t) {
        int at = p.text().indexOf(profileName);
        if (at > 0) out.append(Component.literal(p.text().substring(0, at)).setStyle(p.style()));
        out.append(colored(profileName, p.style(), style, t));
        int end = at + profileName.length();
        if (end < p.text().length()) out.append(Component.literal(p.text().substring(end)).setStyle(p.style()));
    }

    private static List<Part> parts(Component c) {
        List<Part> out = new ArrayList<>();
        c.visit((style, text) -> {
            if (!text.isEmpty()) out.add(new Part(style, text));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static int find(List<Part> parts, String profileName) {
        for (int i = 0; i < parts.size(); i++) {
            Part p = parts.get(i);
            if (!p.text().contains(profileName)) continue;
            return neutral(p.style().getColor()) ? i : -1;
        }
        return -1;
    }

    private static boolean neutral(TextColor color) {
        return color == null || (color.getValue() & 0xFFFFFF) == 0xFFFFFF;
    }
}
