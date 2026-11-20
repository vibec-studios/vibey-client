package dev.henny.vui.text;

import java.util.List;

public final class TextEffectChain implements TextEffect {
    private final TextEffect[] effects;
    private final String source;

    public TextEffectChain(List<TextEffect> effects, String source) {
        this.effects = effects.toArray(new TextEffect[0]);
        this.source = source;
    }

    @Override
    public void apply(GlyphState glyph) {
        for (TextEffect e : effects) e.apply(glyph);
    }

    @Override
    public boolean animated() {
        for (TextEffect e : effects) if (e.animated()) return true;
        return false;
    }

    public int size() {
        return effects.length;
    }

    public TextEffect get(int i) {
        return effects[i];
    }

    @Override
    public String toString() {
        return source;
    }
}
