package dev.henny.vibeyclient.module.clips.encode;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.Vibey;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Finds the first encoder that works here with a 2-frame test encode at the clip size (~0.3 s each) and caches the
 * answer per (FFmpeg build, GPU) in {@code config/vibeyclient/clips-encoder.json}. Blocking: call off the render thread.
 */
public final class EncoderProbe {
    private EncoderProbe() {}

    public static boolean windows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /** Cache key: the FFmpeg file (path, size, date) and the GPU. */
    public static String key(String ffmpeg, String gpu) {
        long size = 0, mtime = 0;
        try {
            Path p = Path.of(ffmpeg);
            size = Files.size(p);
            mtime = Files.getLastModifiedTime(p).toMillis();
        } catch (IOException | RuntimeException ignored) {
            // keep zeros
        }
        return ffmpeg + "|" + size + "|" + mtime + "|" + gpu;
    }

    /** The cached working encoder for {@code key}, or null. */
    public static Encoder cached(Path cacheFile, String key) {
        try {
            JsonObject o = JsonParser.parseString(Files.readString(cacheFile, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!key.equals(o.get("key").getAsString())) return null;
            return Encoder.byId(o.get("encoder").getAsString());
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    public static void store(Path cacheFile, String key, Encoder e, List<String> failed) {
        JsonObject o = new JsonObject();
        o.addProperty("key", key);
        o.addProperty("encoder", e.id());
        o.addProperty("codec", e.codec);
        o.addProperty("failed", String.join(",", failed));
        try {
            Files.createDirectories(cacheFile.getParent());
            Files.writeString(cacheFile, o.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            Vibey.LOG.debug("clips encoder cache not written: {}", ex.toString());
        }
    }

    /** Result of {@link #probe}: the encoder that worked (null = none) and the ones that failed. */
    public record Result(Encoder encoder, List<String> failed) {}

    /**
     * Tries {@code order} minus {@code skip} at {@code w}×{@code h}. {@code onlyHardware}: software encoders are left
     * out (1080p and 60 fps need hardware, docs/CLIPS.md §2.2).
     */
    public static Result probe(String ffmpeg, List<Encoder> order, Set<Encoder> skip, int w, int h, int fps, boolean onlyHardware) {
        List<String> failed = new ArrayList<>();
        for (Encoder e : order) {
            if (skip.contains(e) || (onlyHardware && !e.hardware)) continue;
            if (works(ffmpeg, e, w, h, fps)) return new Result(e, failed);
            failed.add(e.id());
        }
        return new Result(null, failed);
    }

    public static boolean works(String ffmpeg, Encoder e, int w, int h, int fps) {
        List<String> c = new ArrayList<>(List.of(ffmpeg, "-hide_banner", "-nostdin", "-loglevel", "error",
                "-f", "lavfi", "-i", "color=black:s=" + w + "x" + h + ":r=" + fps, "-frames:v", "3", "-pix_fmt", "nv12"));
        c.addAll(e.args(fps, 6000));
        c.addAll(List.of("-f", "null", "-"));
        try {
            Process p = new ProcessBuilder(c).redirectErrorStream(true).start();
            byte[] out = p.getInputStream().readAllBytes(); // drains; ffmpeg exits by itself
            if (!p.waitFor(15, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return false;
            }
            boolean ok = p.exitValue() == 0;
            if (!ok) {
                String msg = new String(out, StandardCharsets.UTF_8).strip();
                Vibey.LOG.info("Clips: encoder {} not usable here: {}", e.codec, msg.length() > 200 ? msg.substring(0, 200) : msg);
            }
            return ok;
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
