package dev.henny.vibeyclient.client.text;

import net.minecraft.network.chat.Style;

import java.util.function.UnaryOperator;

public record TextEdit(int start, int end, String replacement, UnaryOperator<Style> restyle) {
    public static TextEdit replace(int start, int end, String replacement) {
        return new TextEdit(start, end, replacement, null);
    }

    public static TextEdit restyle(int start, int end, UnaryOperator<Style> restyle) {
        return new TextEdit(start, end, null, restyle);
    }
}
