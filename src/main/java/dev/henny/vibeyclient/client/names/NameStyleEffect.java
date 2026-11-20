package dev.henny.vibeyclient.client.names;

import dev.henny.vibeyclient.client.ranks.Ranks;
import dev.henny.vibeyclient.ranks.NameStyleDef;
import dev.henny.vui.text.GlyphState;
import dev.henny.vui.text.TextEffect;
import dev.henny.vui.text.TextEffects;

public record NameStyleEffect(String styleId) implements TextEffect {
    public static final String NAME = "namestyle";

    public static void register() {
        TextEffects.register(NAME, args -> new NameStyleEffect(args.isEmpty() ? "" : args.get(0).trim()));
    }

    public static String css(String styleId) {
        return styleId == null || styleId.isEmpty() ? "none" : NAME + "(" + styleId + ")";
    }

    @Override
    public void apply(GlyphState g) {
        NameStyleDef def = Ranks.style(styleId);
        if (def == null || def.style() == null) return;
        int rgb = def.style().colorAt(g.index, g.count, g.time);
        g.color = (g.color & 0xFF000000) | (rgb & 0xFFFFFF);
    }
}
