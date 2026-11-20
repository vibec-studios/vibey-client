package dev.henny.vibeyclient.module.screens.apply;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class WidgetKeys {
    static final int MAX_TEXT = 40;

    private WidgetKeys() {}

    public static String key(Component c) {
        return key(c, 0);
    }

    private static String key(Component c, int depth) {
        if (c == null || depth > 4) return null;
        ComponentContents contents = c.getContents();
        if (contents instanceof TranslatableContents t) {
            Object[] args = t.getArgs();
            if (args.length > 0 && args[0] instanceof Component first && composite(t.getKey())) {
                String inner = key(first, depth + 1);
                if (inner != null) return inner;
            }
            return t.getKey();
        }
        if (contents instanceof PlainTextContents p) {
            String text = p.text().strip();
            if (!text.isEmpty()) return text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text;
        }
        for (Component s : c.getSiblings()) {
            String k = key(s, depth + 1);
            if (k != null) return k;
        }
        return null;
    }

    static boolean composite(String key) {
        return key.startsWith("options.") && (key.endsWith("_value") || key.endsWith(".composed") || key.equals("options.multiplier"))
                || key.startsWith("gui.narrate.") || key.equals("chat.square_brackets");
    }
}
