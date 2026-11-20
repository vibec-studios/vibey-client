package dev.henny.vibeyclient.media.ffmpeg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The API-free fallback of the FFmpeg download: picking the build out of the release's checksum list. */
class FfmpegReleaseTest {
    private static final String SUMS = String.join("\n",
            "aaa1  ffmpeg-master-latest-win64-lgpl-shared.zip",
            "bbb2  ffmpeg-n7.1-latest-win64-lgpl-shared-7.1.zip",
            "CCC3  ffmpeg-n8.0-latest-win64-lgpl-shared-8.0.zip",
            "ddd4  ffmpeg-n8.0-latest-win64-gpl-shared-8.0.zip",
            "eee5 *ffmpeg-n8.0-latest-winarm64-lgpl-shared-8.0.zip",
            "fff6  ffmpeg-n8.0-latest-linux64-lgpl-shared-8.0.tar.xz", "");

    @Test
    void picksTheNewestLgplSharedBuildForThePlatform() {
        FfmpegAsset a = FfmpegRelease.pickFromChecksums(SUMS, "win64");
        assertEquals("ffmpeg-n8.0-latest-win64-lgpl-shared-8.0.zip", a.name());
        assertEquals("ccc3", a.sha256());
        assertEquals("https://github.com/BtbN/FFmpeg-Builds/releases/latest/download/ffmpeg-n8.0-latest-win64-lgpl-shared-8.0.zip", a.url());
        assertEquals("ffmpeg-n8.0-latest-winarm64-lgpl-shared-8.0.zip", FfmpegRelease.pickFromChecksums(SUMS, "winarm64").name());
        assertNull(FfmpegRelease.pickFromChecksums("ddd4  ffmpeg-n8.0-latest-win64-gpl-shared-8.0.zip", "win64"), "never a GPL build");
        assertNull(FfmpegRelease.pickFromChecksums(SUMS, null));
    }
}
