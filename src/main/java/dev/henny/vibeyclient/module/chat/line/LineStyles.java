package dev.henny.vibeyclient.module.chat.line;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class LineStyles {
    public static final int TIME = 0x8A8A9A;
    public static final int COUNT = 0x9B9BB0;
    public static final int SEARCH = 0x3FE0C5;

    private LineStyles() {}

    public static Style mention(Style s) {
        return s.withColor(TextColor.fromRgb(MentionTag.COLOR)).withBold(true);
    }

    public static Style search(Style s) {
        return s.withColor(TextColor.fromRgb(SEARCH)).withUnderlined(true);
    }

    public static Style dim(int rgb) {
        return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
    }
}
