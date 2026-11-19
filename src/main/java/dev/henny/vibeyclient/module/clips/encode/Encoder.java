package dev.henny.vibeyclient.module.clips.encode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The H.264 encoders we try, hardware first (docs/CLIPS.md §2.2). Every one gets the same stream layout: no B-frames,
 * a forced IDR keyframe exactly every 2 s (the ring cuts there), VBR with a 1.5× peak.
 */
public enum Encoder {
    NVENC("h264_nvenc", true, "NVIDIA NVENC"),
    AMF("h264_amf", true, "AMD AMF"),
    QSV("h264_qsv", true, "Intel Quick Sync"),
    MF_HW("h264_mf", true, "Windows Media Foundation"),
    MF_SW("h264_mf", false, "Windows (software)"),
    X264("libx264", false, "x264 (software)"),
    OPENH264("libopenh264", false, "OpenH264 (software)");

    public final String codec;
    public final boolean hardware;
    public final String label;

    Encoder(String codec, boolean hardware, String label) {
        this.codec = codec;
        this.hardware = hardware;
        this.label = label;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Encoder byId(String id) {
        for (Encoder e : values()) if (e.id().equals(id)) return e;
        return null;
    }

    /** The probe order on this OS. */
    public static List<Encoder> order(boolean windows) {
        if (windows) return List.of(NVENC, AMF, QSV, MF_HW, MF_SW, X264, OPENH264);
        return List.of(NVENC, X264, OPENH264);
    }

    /** Encoder options (after {@code -i}), without output format. */
    public List<String> args(int fps, int kbps) {
        List<String> a = new ArrayList<>();
        a.add("-c:v");
        a.add(codec);
        switch (this) {
            case NVENC -> a.addAll(List.of("-preset", "p2", "-tune", "ll", "-rc", "vbr", "-forced-idr", "1", "-profile:v", "high"));
            case AMF -> a.addAll(List.of("-usage", "lowlatency", "-quality", "speed", "-rc", "vbr_peak", "-profile:v", "high"));
            case QSV -> a.addAll(List.of("-preset", "veryfast", "-look_ahead", "0", "-forced_idr", "1", "-profile:v", "high"));
            case MF_HW -> a.addAll(List.of("-hw_encoding", "1", "-rate_control", "cbr"));
            case MF_SW -> a.addAll(List.of("-hw_encoding", "0", "-rate_control", "cbr", "-threads", "2"));
            case X264 -> a.addAll(List.of("-preset", "veryfast", "-tune", "zerolatency", "-threads", "2"));
            case OPENH264 -> a.addAll(List.of("-profile:v", "main", "-allow_skip_frames", "0", "-threads", "2"));
        }
        int gop = fps * 2;
        a.addAll(List.of("-bf", "0", "-g", String.valueOf(gop), "-keyint_min", String.valueOf(gop),
                "-force_key_frames", "expr:gte(t,n_forced*2)",
                "-b:v", kbps + "k", "-maxrate", (kbps * 3 / 2) + "k", "-bufsize", (kbps * 2) + "k",
                "-color_range", "tv", "-colorspace", "bt709", "-color_primaries", "bt709", "-color_trc", "bt709"));
        return a;
    }
}
