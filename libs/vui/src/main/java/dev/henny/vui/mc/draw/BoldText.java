package dev.henny.vui.mc.draw;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

final class BoldText {
    private static final int LIMIT = 512;

    private final Map<String, Component> cache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Component> eldest) {
            return size() > LIMIT;
        }
    };

    Component of(String text) {
        Component c = cache.get(text);
        if (c == null) {
            c = Component.literal(text).withStyle(ChatFormatting.BOLD);
            cache.put(text, c);
        }
        return c;
    }
}
