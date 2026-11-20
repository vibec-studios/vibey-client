package dev.henny.vibeyclient.ui;

import java.util.ArrayList;
import java.util.List;

public final class TextWrap {
    private TextWrap() {}

    public static List<String> all(Gfx g, String text, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (g.width(next) > width && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }
}
