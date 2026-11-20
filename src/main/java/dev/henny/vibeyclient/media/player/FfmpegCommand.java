package dev.henny.vibeyclient.media.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class FfmpegCommand {
    static final int RATE = 48000;

    private FfmpegCommand() {}

    static List<String> build(FfmpegSpec spec, long startMs, boolean video) {
        List<String> c = new ArrayList<>();
        c.add(spec.ffmpeg());
        c.add("-hide_banner");
        c.add("-nostdin");
        c.add("-loglevel");
        c.add(video ? "info" : "error");
        String lower = spec.uri().toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            c.addAll(List.of("-reconnect", "1", "-reconnect_streamed", "1", "-reconnect_delay_max", "5"));
        }
        if (startMs > 0) {
            c.add("-ss");
            c.add(String.format(Locale.ROOT, "%.3f", startMs / 1000.0));
        }
        c.add("-i");
        c.add(spec.uri());
        c.addAll(video ? videoOutput(spec) : audioOutput());
        return c;
    }

    private static List<String> videoOutput(FfmpegSpec spec) {
        int w = spec.w();
        int h = spec.h();
        return List.of("-map", "0:v:0?", "-an", "-sn", "-dn", "-vf",
                "fps=" + spec.fps() + ",scale=" + w + ":" + h + ":force_original_aspect_ratio=decrease:flags=bilinear,pad="
                        + w + ":" + h + ":-1:-1:color=black,format=rgba",
                "-f", "rawvideo", "-pix_fmt", "rgba", "pipe:1");
    }

    private static List<String> audioOutput() {
        return List.of("-map", "0:a:0?", "-vn", "-sn", "-dn", "-ac", "1", "-ar", String.valueOf(RATE), "-f", "s16le", "pipe:1");
    }
}
