package dev.henny.r3.render;

public interface TextMetrics {
    float advance(int codepoint);

    float lineHeight();

    default float width(String text) {
        float w = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            w += advance(cp);
            i += Character.charCount(cp);
        }
        return w;
    }

    static TextMetrics monospace(float advance, float lineHeight) {
        return new TextMetrics() {
            @Override
            public float advance(int codepoint) {
                return codepoint == '\n' ? 0 : advance;
            }

            @Override
            public float lineHeight() {
                return lineHeight;
            }
        };
    }
}
