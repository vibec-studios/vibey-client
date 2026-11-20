package dev.henny.vibeyclient.module.media.lyrics.world;

import dev.henny.vibeyclient.module.media.lyrics.LyricsConfig;

enum LyricsMode {
    SKY(0.013f, 140),
    FRONT(0.0075f, 150),
    WORLD(0.07f, 220);

    private final float perBlock;
    private final float maxWidth;

    LyricsMode(float perBlock, float maxWidth) {
        this.perBlock = perBlock;
        this.maxWidth = maxWidth;
    }

    static LyricsMode of(LyricsConfig.Position p) {
        return switch (p) {
            case FRONT -> FRONT;
            case WORLD -> WORLD;
            default -> SKY;
        };
    }

    float scale(double distance, float size) {
        return this == WORLD ? perBlock * size : (float) (distance * perBlock * size);
    }

    float maxWidth() {
        return maxWidth;
    }
}
