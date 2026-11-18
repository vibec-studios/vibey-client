package dev.henny.vibeyclient.module.media;

import dev.henny.vibeyclient.module.media.lyrics.LyricsFeature;

/**
 * The parts of the media module, in settings order. Add a feature here (one line); its package is
 * {@code module.media.<id>} (see docs/MEDIA.md).
 */
final class MediaFeatures {
    private MediaFeatures() {}

    static void registerAll(MediaModule m) {
        m.addFeature(new LyricsFeature(m));
    }
}
