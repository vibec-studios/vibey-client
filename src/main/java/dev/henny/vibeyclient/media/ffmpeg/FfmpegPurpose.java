package dev.henny.vibeyclient.media.ffmpeg;

public enum FfmpegPurpose {
    PLAYBACK("playback"),
    RECORDING("recording");

    final String key;

    FfmpegPurpose(String key) {
        this.key = key;
    }
}
