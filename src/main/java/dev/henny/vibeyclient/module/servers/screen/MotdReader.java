package dev.henny.vibeyclient.module.servers.screen;

import dev.henny.vibeyclient.module.servers.core.LegacyFormat;
import dev.henny.vibeyclient.module.servers.core.MotdSegment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class MotdReader {
    static final String DEFAULT = "#a1a1aa";

    private MotdReader() {}

    static List<MotdSegment> read(Component c) {
        List<MotdSegment> out = new ArrayList<>();
        if (c == null) return out;
        c.visit((style, text) -> {
            out.addAll(LegacyFormat.split(new MotdSegment(text, color(style), style.isBold(), style.isItalic())));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static String color(Style style) {
        TextColor col = style.getColor();
        return col == null ? DEFAULT : String.format(Locale.ROOT, "#%06x", col.getValue() & 0xFFFFFF);
    }
}
