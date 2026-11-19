package dev.henny.vibeyclient.module.clips;

import dev.henny.vibeyclient.module.clips.capture.FrameGrabber;
import dev.henny.vibeyclient.module.clips.encode.EncodeSession;
import dev.henny.vibeyclient.module.clips.ring.ClipRing;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The measuring overlay ({@code -Dvibeyclient.clips.stats=true} or the advanced setting, docs/CLIPS.md §1.4):
 * render-thread cost per capture, readback latency, skipped frames, writer repeats, FFmpeg CPU and memory, ring size.
 * Values refresh once a second; {@link #lines()} is also what the game test logs.
 */
public final class ClipsStats {
    private ClipsStats() {}

    private static long lastAt;
    private static long lastCpuMs = -1;
    private static long lastCpuPid;
    private static double cpuPercentOfCore = -1;
    private static long rss = -1;
    private static List<String> lines = List.of();

    /** FFmpeg CPU in % of one core over the last second (-1 unknown). */
    public static double cpuPercentOfCore() {
        return cpuPercentOfCore;
    }

    public static long rssBytes() {
        return rss;
    }

    static void render(Gfx g, Recorder r) {
        refresh(r);
        int y = 40;
        g.rect(2, y - 2, 200, lines.size() * 10 + 4, 0xA0000000);
        for (String l : lines) {
            g.text(l, 4, y, 0xFFE0E0E0);
            y += 10;
        }
    }

    public static List<String> lines() {
        ClipsModule m = ClipsModule.get();
        if (m != null) refresh(m.recorder);
        return lines;
    }

    private static void refresh(Recorder r) {
        long now = System.currentTimeMillis();
        if (now - lastAt < 1000) return;
        long dt = lastAt == 0 ? 1000 : now - lastAt;
        lastAt = now;
        EncodeSession s = r.session();
        if (s != null) {
            long cpu = s.cpuMs();
            if (cpu >= 0 && lastCpuMs >= 0 && lastCpuPid == s.pid()) cpuPercentOfCore = (cpu - lastCpuMs) * 100.0 / dt;
            lastCpuMs = cpu;
            lastCpuPid = s.pid();
            rss = s.rssBytes();
        } else {
            lastCpuMs = -1;
            cpuPercentOfCore = -1;
            rss = -1;
        }
        FrameGrabber f = r.grabber();
        ClipRing ring = r.ring();
        List<String> out = new ArrayList<>();
        out.add("Clips: " + r.state() + (r.pause() != Recorder.Pause.NONE ? " (" + r.pause() + ")" : "")
                + "  game " + Minecraft.getInstance().getFps() + " fps, avg " + Math.round(r.fps.average));
        out.add("encoder " + (r.encoder() == null ? "-" : r.encoder().codec) + (s == null ? "" : "  " + s.width + "x" + s.height + "@" + s.fps));
        if (f != null) {
            out.add(String.format(Locale.ROOT, "capture %d  skipped %d/%d  latency %d fr", f.captured, f.skippedNoSlot, f.skippedNoFrame, f.lastLatencyFrames));
            out.add(String.format(Locale.ROOT, "render thread %.3f ms avg, %.2f ms max (issue %.3f, readback %.3f, %s; copy thread %.3f)",
                    f.renderMsAvg, f.renderMsMax, f.issueMsAvg, f.mapMsAvg, FrameGrabber.mode, f.copyMsAvg));
        }
        if (s != null) {
            out.add(String.format(Locale.ROOT, "written %d  repeated %d  superseded %d  write max %d ms", s.framesWritten(),
                    s.framesRepeated(), s.framesSuperseded(), s.writeMsMax()));
            out.add(String.format(Locale.ROOT, "ffmpeg cpu %.1f %% of a core (%.2f %% CPU)  rss %d MB", cpuPercentOfCore,
                    cpuPercentOfCore / Runtime.getRuntime().availableProcessors(), rss / 1048576));
        }
        if (ring != null) {
            out.add(String.format(Locale.ROOT, "ring %.1f MB  %.1f s  %d chunks", ring.bytes() / 1048576.0, ring.durationMs() / 1000.0, ring.chunks()));
        }
        Runtime rt = Runtime.getRuntime();
        out.add(String.format(Locale.ROOT, "java heap %d / %d MB", (rt.totalMemory() - rt.freeMemory()) / 1048576, rt.maxMemory() / 1048576));
        lines = out;
    }
}
