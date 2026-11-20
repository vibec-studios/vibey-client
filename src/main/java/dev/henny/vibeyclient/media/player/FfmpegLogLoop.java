package dev.henny.vibeyclient.media.player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FfmpegLogLoop implements Runnable {
    private static final Pattern DURATION = Pattern.compile("Duration: (\\d+):(\\d+):(\\d+)\\.(\\d+)");
    private static final List<String> NOISE = List.of("Stream", "Input", "Output", "Duration", "Metadata", "frame=");
    private static final List<String> ERRORS = List.of("rror", "No such", "Invalid", "not found", "denied", "refused");

    private final FfmpegSession s;

    FfmpegLogLoop(FfmpegSession s) {
        this.s = s;
    }

    @Override
    public void run() {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(s.video.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                readDuration(line);
                readError(line.trim());
            }
        } catch (IOException ignored) {}
    }

    private void readDuration(String line) {
        Matcher m = DURATION.matcher(line);
        if (!m.find() || s.durationMs >= 0) return;
        long ms = (Long.parseLong(m.group(1)) * 3600 + Long.parseLong(m.group(2)) * 60 + Long.parseLong(m.group(3))) * 1000
                + Long.parseLong((m.group(4) + "00").substring(0, 3));
        if (ms > 0) s.durationMs = ms;
    }

    private void readError(String t) {
        if (t.isEmpty() || NOISE.stream().anyMatch(t::startsWith)) return;
        if (ERRORS.stream().anyMatch(t::contains)) s.lastError = t.length() > 160 ? t.substring(0, 160) : t;
    }
}
