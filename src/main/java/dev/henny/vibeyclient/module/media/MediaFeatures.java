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
        // Video screens: only in the Fabric 1.21.11 jar for now (gradle/common.gradle leaves the package out elsewhere).
        addIfPresent(m, "dev.henny.vibeyclient.module.media.video.VideoFeature");
    }

    /** Adds a feature whose package is only part of some builds. */
    private static void addIfPresent(MediaModule m, String className) {
        try {
            Class<?> c = Class.forName(className);
            m.addFeature((MediaFeature) c.getConstructor(MediaModule.class).newInstance(m));
        } catch (ClassNotFoundException absent) {
            // not in this jar
        } catch (ReflectiveOperationException | LinkageError e) {
            dev.henny.vibeyclient.Vibey.LOG.error("Could not create media feature {}", className, e);
        }
    }
}
