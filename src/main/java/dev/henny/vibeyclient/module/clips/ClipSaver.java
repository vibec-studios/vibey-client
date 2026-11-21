package dev.henny.vibeyclient.module.clips;

import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.launcher.LauncherStatus;
import dev.henny.vibeyclient.launcher.SessionFile;
import dev.henny.vibeyclient.module.clips.encode.Encoder;
import dev.henny.vibeyclient.module.clips.ring.Chunk;
import dev.henny.vibeyclient.module.clips.ring.ClipRing;
import dev.henny.vibeyclient.module.clips.store.ClipMeta;
import dev.henny.vibeyclient.module.clips.store.ClipStore;
import dev.henny.vibeyclient.module.clips.store.ClipTimeline;
import dev.henny.vibeyclient.module.clips.ui.ClipsGalleryScreen;
import dev.henny.vibeyclient.media.ffmpeg.Ffmpeg;
import dev.henny.vibeyclient.ui.toast.ToastStack;
import net.minecraft.client.Minecraft;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Turns the newest part of the ring into a clip (docs/CLIPS.md §3.2, §10.3): GOP chunks → one {@code .ts} part per
 * FFmpeg session → {@code ffmpeg -f concat -c copy -movflags +faststart} → {@code .<id>.mp4.part} → {@code <id>.mp4},
 * thumbnail via {@code .part}, then {@code <id>.json} last. Runs on the clips I/O thread; the buffer keeps recording.
 */
public final class ClipSaver {
    private ClipSaver() {}

    /** What an automatic trigger knows: reason, kills, event wall-clock times (ms) and victims. */
    public record AutoInfo(String reason, int kills, List<Long> eventTimes, List<String> eventTypes, List<String> victims, long highlightAt) {}

    /** Metadata the game knows only on the client thread. */
    private record Context(String mode, String server, String world, String player, String uuid) {}

    private static volatile boolean saving;

    public static boolean saving() {
        return saving;
    }

    /**
     * Saves the last {@code lengthMs} (client thread). {@code settleMs}: wait so the encoder can flush the newest
     * frames first (hotkey; auto clips already waited for their post-roll).
     */
    static void save(ClipsModule m, ClipRing ring, String reason, long lengthMs, AutoInfo auto, Encoder encoder, long settleMs) {
        save(m, ring, reason, lengthMs, auto, encoder, settleMs, -1);
    }

    static void save(ClipsModule m, ClipRing ring, String reason, long lengthMs, AutoInfo auto, Encoder encoder, long settleMs,
                     long trimHintMs) {
        if (saving) {
            Notify.show(new Notify.Toast(Lang.tr("clips.toast.busy"), null, Notify.Kind.INFO, null, null));
            return;
        }
        String ffmpeg = Ffmpeg.find();
        if (ffmpeg == null || ring == null) return;
        saving = true;
        Context ctx = context(m);
        long requestedAt = System.currentTimeMillis();
        int fps = m.rate.get().fps;
        int w = m.resolution.get().width, h = m.resolution.get().height;
        ClipsFiles.io(() -> {
            try {
                if (settleMs > 0) Thread.sleep(settleMs);
                ClipStore.Clip c = write(m, ffmpeg, ring, reason, lengthMs, auto, encoder, ctx, requestedAt, w, h, fps, trimHintMs);
                ClipsFiles.cleanup(m);
                Tasks.onMain(() -> saved(m, c));
            } catch (Exception e) {
                Vibey.LOG.warn("Clips: saving failed", e);
                String why = e.getMessage() == null ? e.toString() : e.getMessage();
                Notify.show(new Notify.Toast(Lang.tr("clips.toast.failed"), why, Notify.Kind.ERROR, null, null));
            } finally {
                saving = false;
            }
        });
    }

    private static Context context(ClipsModule m) {
        Minecraft mc = Minecraft.getInstance();
        SessionFile.Snapshot s = LauncherStatus.sample(); // server already hidden for discord.server / "Hide server address"
        String mode = s.state() == SessionFile.State.MENU ? null : s.state().id();
        String server = m.saveServer.on() ? s.server() : null;
        String name = null, uuid = null;
        try {
            name = mc.getUser().getName();
            uuid = mc.getUser().getProfileId() == null ? null : mc.getUser().getProfileId().toString();
        } catch (RuntimeException ignored) {
            // offline dev user
        }
        return new Context(mode, server, s.world(), name, uuid);
    }

    private static ClipStore.Clip write(ClipsModule m, String ffmpeg, ClipRing ring, String reason, long lengthMs, AutoInfo auto,
                                        Encoder encoder, Context ctx, long requestedAt, int w, int h, int fps, long trimHintMs) throws Exception {
        List<Chunk.View> views = ring.last(lengthMs);
        long endWall = System.currentTimeMillis();
        if (views.isEmpty()) throw new IOException(Lang.tr("clips.error.empty"));
        ClipStore store = ClipsFiles.store();
        Files.createDirectories(store.dir());
        String id = store.newId(LocalDateTime.now(), reason);
        Path tmp = store.tmpDir().resolve(id);
        Files.createDirectories(tmp);
        Path video = store.dir().resolve(id + ".mp4");
        Path videoPart = store.dir().resolve("." + id + ".mp4.part");
        try {
            // 1. one .ts per encoder session (timestamps restart per session; concat re-bases them per file)
            List<Path> parts = new ArrayList<>();
            OutputStream out = null;
            int session = Integer.MIN_VALUE;
            long durationMs = 0;
            ClipTimeline.Builder timeline = new ClipTimeline.Builder();
            try {
                for (Chunk.View v : views) {
                    if (v.session() != session || out == null) {
                        if (out != null) {
                            out.close();
                            timeline.cut(durationMs);
                        }
                        Path p = tmp.resolve("part" + parts.size() + ".ts");
                        parts.add(p);
                        out = new BufferedOutputStream(Files.newOutputStream(p), 1 << 16);
                        out.write(v.headers());
                        session = v.session();
                    }
                    long at = durationMs;
                    if (v.writeTo(out)) {
                        timeline.keyframe(at);
                        durationMs += v.durationMs();
                    }
                }
            } finally {
                if (out != null) out.close();
            }
            if (durationMs < 500) throw new IOException(Lang.tr("clips.error.empty"));
            // 2. remux without re-encoding
            List<String> cmd = new ArrayList<>(List.of(ffmpeg, "-hide_banner", "-nostdin", "-loglevel", "error", "-y"));
            if (parts.size() == 1) {
                cmd.addAll(List.of("-i", parts.get(0).toString()));
            } else {
                Path list = tmp.resolve("list.txt");
                StringBuilder sb = new StringBuilder();
                for (Path p : parts) sb.append("file '").append(p.getFileName().toString()).append("'\n");
                Files.writeString(list, sb.toString(), StandardCharsets.UTF_8);
                cmd.addAll(List.of("-f", "concat", "-safe", "0", "-i", list.toString()));
            }
            cmd.addAll(List.of("-map", "0", "-c", "copy", "-movflags", "+faststart", "-f", "mp4", videoPart.toString()));
            run(cmd, (int) Math.max(60, 60 + durationMs / 5000));
            if (!Files.isRegularFile(videoPart) || Files.size(videoPart) == 0) throw new IOException("FFmpeg wrote no video");
            ClipStore.move(videoPart, video);

            // 3. events relative to the clip start, highlight, thumbnail
            long startWall = endWall - durationMs;
            List<ClipMeta.Event> events = new ArrayList<>();
            long highlight = -1;
            int kills = 0;
            if (auto != null) {
                kills = auto.kills();
                for (int i = 0; i < auto.eventTimes().size(); i++) {
                    long t = auto.eventTimes().get(i) - startWall;
                    events.add(new ClipMeta.Event(Math.max(0, Math.min(durationMs, t)), auto.eventTypes().get(i), auto.victims().get(i)));
                }
                highlight = Math.max(0, Math.min(durationMs, auto.highlightAt() - startWall));
            }
            long thumbAt = highlight >= 0 ? highlight : Math.max(0, durationMs - 1500);
            boolean thumb = thumbnail(ffmpeg, video, store.dir(), id, thumbAt);

            // 4. the JSON last: from now on the clip exists for everyone
            ClipMeta.Info info = new ClipMeta.Info(id, Instant.ofEpochMilli(endWall), durationMs, Files.size(video), w, h, fps, false,
                    reason, kills, highlight, events, ctx.player(), ctx.uuid(), ctx.mode(), ctx.server(), ctx.world(),
                    BuildInfo.MINECRAFT, "fabric", BuildInfo.VERSION, encoder == null ? null : encoder.codec, thumb);
            com.google.gson.JsonObject json = ClipMeta.json(info);
            ClipTimeline.write(json, timeline.build(durationMs, trimHintMs));
            store.writeJson(id, json);
            Vibey.LOG.info("Clips: saved {} ({} ms, {} KB, {} part(s), {} ms after the request)", id, durationMs,
                    Files.size(video) / 1024, parts.size(), System.currentTimeMillis() - requestedAt);
            return store.get(id);
        } catch (Exception e) {
            Files.deleteIfExists(videoPart);
            if (!Files.exists(store.dir().resolve(id + ".json"))) {
                Files.deleteIfExists(video);
                Files.deleteIfExists(store.dir().resolve(id + ".jpg"));
            }
            throw e;
        } finally {
            deleteTree(tmp);
        }
    }

    /** {@code <id>.jpg} at {@code atMs}, max 480 px wide; false when it could not be made (the JSON then has none). */
    static boolean thumbnail(String ffmpeg, Path video, Path dir, String id, long atMs) {
        Path part = dir.resolve("." + id + ".jpg.part");
        try {
            run(List.of(ffmpeg, "-hide_banner", "-nostdin", "-loglevel", "error", "-y", "-ss",
                    String.format(Locale.ROOT, "%.3f", atMs / 1000.0), "-i", video.toString(), "-frames:v", "1",
                    "-vf", "scale=480:-2", "-q:v", "4", "-f", "image2", "-c:v", "mjpeg", part.toString()), 20);
            if (!Files.isRegularFile(part) || Files.size(part) == 0) {
                // seeking past the end of a very short clip: take the first frame
                run(List.of(ffmpeg, "-hide_banner", "-nostdin", "-loglevel", "error", "-y", "-i", video.toString(),
                        "-frames:v", "1", "-vf", "scale=480:-2", "-q:v", "4", "-f", "image2", "-c:v", "mjpeg", part.toString()), 20);
            }
            if (!Files.isRegularFile(part) || Files.size(part) == 0) return false;
            ClipStore.move(part, dir.resolve(id + ".jpg"));
            return true;
        } catch (Exception e) {
            Vibey.LOG.debug("Clips: no thumbnail for {}: {}", id, e.toString());
            try {
                Files.deleteIfExists(part);
            } catch (IOException ignored) {
                // left for the orphan cleanup
            }
            return false;
        }
    }

    static void run(List<String> cmd, int timeoutS) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        byte[] out = p.getInputStream().readAllBytes();
        if (!p.waitFor(timeoutS, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IOException("FFmpeg took too long");
        }
        if (p.exitValue() != 0) {
            String msg = new String(out, StandardCharsets.UTF_8).strip();
            throw new IOException("FFmpeg: " + (msg.length() > 160 ? msg.substring(0, 160) : msg));
        }
    }

    private static void deleteTree(Path p) {
        if (!Files.exists(p)) return;
        try (var s = Files.walk(p)) {
            for (Path q : s.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(q);
        } catch (IOException ignored) {
            // removed at the next start
        }
    }

    // ============================================================================================ toast

    private static void saved(ClipsModule m, ClipStore.Clip c) {
        if (c == null) return;
        String what = Lang.tr("clips.reason." + reasonKey(c.reason()));
        String body = what + " · " + duration(c.durationMs());
        List<ToastStack.Button> buttons = List.of(
                new ToastStack.Button(Lang.tr("clips.toast.view"), true, () -> ClipsGalleryScreen.openClip(null, c.id())),
                new ToastStack.Button(Lang.tr("clips.toast.keep"), false, () -> ClipsFiles.io(() -> {
                    try {
                        ClipsFiles.store().edit(c.id(), true, null, null);
                        Notify.show(new Notify.Toast(Lang.tr("clips.toast.kept"), null, Notify.Kind.SUCCESS, null, null));
                    } catch (IOException e) {
                        Vibey.LOG.warn("Clips: keep failed", e);
                    }
                })));
        ToastStack.show(new Notify.Toast(Lang.tr("clips.toast.saved"), body, Notify.Kind.SUCCESS, null, null), buttons, 8000,
                "clips.saved");
    }

    /** "hotkey", "kill", … or "clip" for values this version doesn't know. */
    public static String reasonKey(String reason) {
        return switch (reason == null ? "" : reason) {
            case "hotkey", "kill", "multikill", "clutch", "death" -> reason;
            default -> "clip";
        };
    }

    /** 0:31, 1:05 */
    public static String duration(long ms) {
        long s = Math.round(ms / 1000.0);
        return s / 60 + ":" + String.format(Locale.ROOT, "%02d", s % 60);
    }
}
