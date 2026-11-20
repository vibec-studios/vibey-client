package dev.henny.vibeyclient.module.clips;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.clips.capture.FrameGrabber;
import dev.henny.vibeyclient.module.clips.encode.EncodeSession;
import dev.henny.vibeyclient.module.clips.encode.Encoder;
import dev.henny.vibeyclient.module.clips.encode.EncoderProbe;
import dev.henny.vibeyclient.module.clips.ring.ClipRing;
import dev.henny.vibeyclient.media.ffmpeg.Ffmpeg;
import dev.henny.vibeyclient.media.ffmpeg.FfmpegPurpose;
import dev.henny.vibeyclient.media.ffmpeg.FfmpegSetupScreen;
import dev.henny.vibeyclient.ui.toast.ToastStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The replay buffer's state machine (client/render thread unless noted). It decides every tick whether recording may
 * run right now (docs/CLIPS.md §7), starts and stops the FFmpeg session, paces captures in the frame hook, and hands
 * saves to {@link ClipSaver}. Everything costly is off unless the buffer is on and the player is actually playing.
 */
public final class Recorder {
    public enum State { OFF, BLOCKED, NEED_FFMPEG, PROBING, NO_ENCODER, NO_WORLD, STARTING, RECORDING, PAUSED, FAILED }

    public enum Pause { NONE, MENU, INVENTORY, UNFOCUSED, MINIMIZED, LOW_FPS, ENCODER_BEHIND }

    private static final long STOP_AFTER_PAUSE_MS = 60_000;
    /** Game tests: the test window usually has no focus; record anyway. */
    public static volatile boolean ignoreFocusForTest = Boolean.getBoolean("vibeyclient.clips.ignoreFocus");
    private static final long RAM_HEADROOM_MS = 2_000;

    private final ClipsModule m;
    private volatile ClipRing ring;
    private volatile EncodeSession session;
    private FrameGrabber grabber;
    private volatile Encoder encoder;
    private volatile String encoderFfmpeg;
    private volatile boolean probing;
    private volatile boolean starting;
    private final Set<Encoder> broken = EnumSet.noneOf(Encoder.class);
    private int sessionIds;
    private volatile State state = State.OFF;
    private volatile Pause pause = Pause.NONE;
    private long pausedSince;
    private long nextCapture;
    private boolean restart;
    private boolean lowFpsToastShown;
    private volatile String failure;
    final FpsWatch fps = new FpsWatch();
    private long behindUntil;
    private long behindWindowStart;
    private long behindCaptured, behindSkipped;
    private long lastStartAttempt;

    Recorder(ClipsModule m) {
        this.m = m;
    }

    // ============================================================================================ queries

    public State state() {
        return state;
    }

    public Pause pause() {
        return pause;
    }

    public ClipRing ring() {
        return ring;
    }

    public EncodeSession session() {
        return session;
    }

    public FrameGrabber grabber() {
        return grabber;
    }

    public Encoder encoder() {
        return encoder;
    }

    public String failure() {
        return failure;
    }

    /** Something is in the buffer that a save could use. */
    public boolean hasFootage() {
        ClipRing r = ring;
        return r != null && r.durationMs() >= 1000;
    }

    /** Settings page line, e.g. "Recording · Intel Quick Sync · 26 MB RAM · 31 s". */
    String statusLine() {
        if (!m.buffer.on()) return null;
        State s = state;
        ClipRing r = ring;
        String held = r == null ? "" : Lang.tr("clips.status.held",
                String.format(Locale.ROOT, r.bytes() < 10_485_760 ? "%.1f" : "%.0f", r.bytes() / 1048576.0), r.durationMs() / 1000);
        Encoder e = encoder;
        String enc = e == null ? "" : " · " + Lang.tr(e.hardware ? "clips.status.hardware" : "clips.status.software", e.label);
        return switch (s) {
            case OFF -> null;
            case BLOCKED -> Lang.tr("clips.status.blocked");
            case NEED_FFMPEG -> Lang.tr("clips.status.needFfmpeg");
            case PROBING -> Lang.tr("clips.status.probing");
            case NO_ENCODER -> Lang.tr("clips.status.noEncoder");
            case NO_WORLD -> Lang.tr("clips.status.noWorld") + enc;
            case STARTING -> Lang.tr("clips.status.starting") + enc;
            case RECORDING -> Lang.tr("clips.status.recording") + enc + held;
            case PAUSED -> Lang.tr("clips.status.paused", Lang.tr("clips.pause." + pause.name().toLowerCase(Locale.ROOT))) + enc + held;
            case FAILED -> Lang.tr("clips.status.failed", failure == null ? "?" : failure);
        };
    }

    /** "30 s → 26 s at this quality" when the RAM cap is smaller than length × bitrate. */
    String capNotice() {
        if (!m.buffer.on()) return null;
        long need = (long) m.bitrateKbps() * 125L * (m.length.get() + 2);
        long cap = m.ramCap.get() * 1048576L;
        if (need <= cap) return null;
        long real = Math.max(2, cap / ((long) m.bitrateKbps() * 125L) - 2);
        return Lang.tr("clips.notice.cap", m.length.get(), real);
    }

    long keepMs() {
        return m.length.get() * 1000L + RAM_HEADROOM_MS;
    }

    long capBytes() {
        return m.ramCap.get() * 1048576L;
    }

    // ============================================================================================ events

    /** The player switched the buffer on/off in the settings (not at startup). */
    void bufferToggled(boolean on) {
        if (!on) return;
        if (!m.hintShown.on()) {
            m.hintShown.set(true);
            String key = ClientCompat.keyName(m.saveKey.mapping()).getString();
            ToastStack.show(new Notify.Toast(Lang.tr("clips.hint.title"), Lang.tr("clips.hint.body", key, m.length.get()),
                    Notify.Kind.INFO, null, null), List.of(), 9000, "clips.hint");
        }
        Ffmpeg.refresh();
        if (!Ffmpeg.available()) setupFfmpeg();
    }

    /** Opens the download prompt (nothing is downloaded before the player agrees there). */
    public void setupFfmpeg() {
        ClientCompat.openScreen(new FfmpegSetupScreen(ClientCompat.screen(), FfmpegPurpose.RECORDING));
    }

    void settingsChanged(boolean needsRestart) {
        ClipRing r = ring;
        if (r != null) r.limits(keepMs(), capBytes());
        if (needsRestart) restart = true;
    }

    void onRulesChanged() {
        if (ServerRules.isDisabled(ClipsModule.RULE_CLIPS) && (ring != null || session != null)) {
            stop(true); // nothing recorded on this server is kept
            Vibey.LOG.info("Clips: switched off by the server, buffer dropped");
        }
    }

    void worldJoined() {
        fps.reset();
        lowFpsToastShown = false;
    }

    void worldLeft() {
        stop(true);
    }

    // ============================================================================================ tick (20/s)

    void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!m.isEnabled() || !m.buffer.on()) {
            if (ring != null || session != null || grabber != null) stop(true);
            state = State.OFF;
            return;
        }
        if (ServerRules.isDisabled(ClipsModule.RULE_CLIPS)) {
            if (ring != null || session != null) stop(true);
            state = State.BLOCKED;
            return;
        }
        if (restart) {
            restart = false;
            stop(false);
            encoder = null; // resolution or encoder choice may have changed: probe again
            broken.clear();
        }
        String ffmpeg = Ffmpeg.find();
        if (ffmpeg == null) {
            state = State.NEED_FFMPEG;
            return;
        }
        if (encoder == null || !ffmpeg.equals(encoderFfmpeg)) {
            if (!probing) probe(ffmpeg);
            state = probing ? State.PROBING : State.NO_ENCODER;
            return;
        }
        if (mc.level == null || mc.player == null) {
            if (session != null) stop(false);
            state = State.NO_WORLD;
            return;
        }
        EncodeSession s = session;
        if (s != null && s.failed()) {
            failure = s.lastError();
            Vibey.LOG.warn("Clips: encoder {} stopped: {}", s.encoder.codec, failure);
            session = null;
            if (!s.producedOutput()) {
                broken.add(s.encoder); // try the next one
                encoder = null;
            }
            s.close(0);
            s = null;
        }

        Pause p = pauseReason(mc);
        long now = System.currentTimeMillis();
        if (p != Pause.NONE) {
            if (pause == Pause.NONE) pausedSince = now;
            pause = p;
            if (p == Pause.MENU || p == Pause.INVENTORY || p == Pause.UNFOCUSED || p == Pause.MINIMIZED) fps.reset();
            if (s != null) {
                s.paused(true);
                if (now - pausedSince > STOP_AFTER_PAUSE_MS) {
                    session = null; // frees FFmpeg's ~200 MB; the ring keeps what it has
                    EncodeSession dead = s;
                    ClipsFiles.io(() -> dead.close(2000));
                }
            }
            if (p == Pause.LOW_FPS && !lowFpsToastShown && s != null) {
                lowFpsToastShown = true;
                Notify.show(new Notify.Toast(Lang.tr("clips.toast.lowFps"), Lang.tr("clips.toast.lowFps.body", m.pauseFps()),
                        Notify.Kind.WARN, null, null));
            }
            state = State.PAUSED;
        } else {
            pause = Pause.NONE;
            pausedSince = 0;
            if (s == null) {
                startSession(ffmpeg);
                state = State.STARTING;
            } else {
                s.paused(false);
                state = s.producedOutput() ? State.RECORDING : State.STARTING;
            }
        }
        ClipRing r = ring;
        if (r != null) r.limits(keepMs(), capBytes());
    }

    private Pause pauseReason(Minecraft mc) {
        if (mc.getWindow().isMinimized()) return Pause.MINIMIZED;
        if (!mc.isWindowActive() && !ignoreFocusForTest) return Pause.UNFOCUSED;
        var screen = mc.screen;
        if (screen != null && !(screen instanceof ChatScreen)) {
            if (screen instanceof AbstractContainerScreen<?>) return m.inInventories.on() ? Pause.NONE : Pause.INVENTORY;
            return Pause.MENU;
        }
        if (mc.getOverlay() != null) return Pause.MENU;
        if (m.lowFpsPause.on() && fps.low(threshold(mc))) return Pause.LOW_FPS;
        if (System.currentTimeMillis() < behindUntil) return Pause.ENCODER_BEHIND;
        return Pause.NONE;
    }

    /** The low-FPS threshold, never above what the game can reach with its own frame cap. */
    private int threshold(Minecraft mc) {
        int t = m.pauseFps();
        int cap = mc.options.framerateLimit().get();
        if (mc.options.enableVsync().get()) {
            int hz = mc.getWindow().getRefreshRate();
            if (hz > 0) cap = Math.min(cap, hz);
        }
        if (cap > 0 && cap < 260) t = Math.min(t, Math.max(10, (int) (cap * 0.85f)));
        return t;
    }

    private void probe(String ffmpeg) {
        probing = true;
        failure = null;
        int w = m.resolution.get().width, h = m.resolution.get().height, f = m.rate.get().fps;
        boolean hwOnly = m.resolution.get() == ClipsModule.Resolution.P1080 || f > 30;
        Encoder chosen = m.encoder.get().encoder;
        String gpu = gpuName();
        Set<Encoder> skip = EnumSet.copyOf(broken.isEmpty() ? EnumSet.noneOf(Encoder.class) : broken);
        Thread t = new Thread(() -> {
            try {
                Encoder result;
                if (chosen != null) {
                    result = EncoderProbe.works(ffmpeg, chosen, w, h, f) ? chosen : null;
                    if (result == null) failure = Lang.tr("clips.error.encoder", chosen.label);
                } else {
                    String key = EncoderProbe.key(ffmpeg, gpu) + "|" + w + "x" + h + "@" + f;
                    Encoder c = skip.isEmpty() ? EncoderProbe.cached(ClipsFiles.encoderCache(), key) : null;
                    if (c != null && hwOnly && !c.hardware) c = null;
                    if (c != null) {
                        result = c;
                    } else {
                        EncoderProbe.Result pr = EncoderProbe.probe(ffmpeg, Encoder.order(EncoderProbe.windows()), skip, w, h, f, hwOnly);
                        result = pr.encoder();
                        if (result != null) EncoderProbe.store(ClipsFiles.encoderCache(), key, result, pr.failed());
                        else failure = Lang.tr(hwOnly ? "clips.error.noHardware" : "clips.error.noEncoder");
                    }
                }
                if (result != null) Vibey.LOG.info("Clips: encoder {} ({}x{}@{})", result.codec, w, h, f);
                encoderFfmpeg = ffmpeg;
                encoder = result;
            } finally {
                probing = false;
            }
        }, "Vibey clips probe");
        t.setDaemon(true);
        t.start();
    }

    private static String gpuName() {
        try {
            var d = com.mojang.blaze3d.systems.RenderSystem.getDevice();
            return d.getVendor() + " " + d.getRenderer();
        } catch (RuntimeException e) {
            return "?";
        }
    }

    private void startSession(String ffmpeg) {
        if (starting) return;
        long now = System.currentTimeMillis();
        if (now - lastStartAttempt < 3000) return; // a failing start is not retried every tick
        lastStartAttempt = now;
        Encoder e = encoder;
        if (e == null) return;
        starting = true;
        if (ring == null) ring = new ClipRing(keepMs(), capBytes());
        ClipRing r = ring;
        int id = ++sessionIds;
        int w = m.resolution.get().width, h = m.resolution.get().height, f = m.rate.get().fps, kbps = m.bitrateKbps();
        Thread t = new Thread(() -> {
            try {
                EncodeSession s = new EncodeSession(id, ffmpeg, e, w, h, f, kbps, r);
                if (ring != r || !m.bufferActive()) {
                    s.close(0); // stopped meanwhile
                    return;
                }
                session = s;
                failure = null;
            } catch (java.io.IOException ex) {
                failure = ex.getMessage();
                Vibey.LOG.warn("Clips: FFmpeg did not start", ex);
            } finally {
                starting = false;
            }
        }, "Vibey clips start");
        t.setDaemon(true);
        t.start();
    }

    /** Stops recording; {@code dropBuffer}: also throws the recorded footage away. Client thread. */
    void stop(boolean dropBuffer) {
        EncodeSession s = session;
        session = null;
        if (s != null) ClipsFiles.io(() -> s.close(1500));
        if (grabber != null) {
            grabber.close();
            grabber = null;
        }
        if (dropBuffer) {
            ClipRing r = ring;
            ring = null;
            if (r != null) r.clear();
        }
        pause = Pause.NONE;
        pausedSince = 0;
    }

    // ============================================================================================ frame hook

    /** Render thread, right after the frame was blitted to the window. */
    public void onFrame() {
        EncodeSession s = session;
        if (s == null && grabber == null) return;
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        if (pause == Pause.NONE || pause == Pause.LOW_FPS) fps.frame(now);
        if (s != null && !s.alive()) s = null;
        if (grabber != null && s != null && (grabber.width() != s.width || grabber.height() != s.height)) {
            grabber.close();
            grabber = null;
        }
        if (grabber == null) {
            if (s == null) return;
            try {
                grabber = new FrameGrabber(s.width, s.height);
            } catch (RuntimeException e) {
                failure = e.getMessage();
                state = State.FAILED;
                Vibey.LOG.error("Clips: could not set up capture", e);
                m.buffer.set(false);
                return;
            }
            nextCapture = now;
        }
        boolean record = s != null && pause == Pause.NONE && !s.paused();
        boolean due = false;
        if (record) {
            long period = 1_000_000_000L / s.fps;
            if (now >= nextCapture) {
                due = true;
                nextCapture += period;
                if (now - nextCapture > period) nextCapture = now + period;
            }
        } else {
            nextCapture = now;
        }
        grabber.frame(mc.getMainRenderTarget(), s, due);
        if (grabber.broken()) {
            failure = Lang.tr("clips.error.capture");
            state = State.FAILED;
            m.buffer.set(false);
            return;
        }
        checkBehind();
    }

    /** More than 10 % of due frames skipped over 5 s: the encoder is behind, pause for 10 s. */
    private void checkBehind() {
        long now = System.currentTimeMillis();
        if (behindWindowStart == 0) {
            behindWindowStart = now;
            behindCaptured = grabber.captured;
            behindSkipped = grabber.skippedNoSlot + grabber.skippedNoFrame;
            return;
        }
        if (now - behindWindowStart < 5000) return;
        long cap = grabber.captured - behindCaptured;
        long skip = grabber.skippedNoSlot + grabber.skippedNoFrame - behindSkipped;
        behindWindowStart = now;
        behindCaptured = grabber.captured;
        behindSkipped = grabber.skippedNoSlot + grabber.skippedNoFrame;
        if (cap + skip > 20 && skip * 10 > cap + skip) {
            behindUntil = now + 10_000;
            Vibey.LOG.info("Clips: encoder behind ({} of {} frames skipped), pausing 10 s", skip, cap + skip);
        }
    }

    // ============================================================================================ saving

    /** The save key. */
    void saveHotkey() {
        if (ServerRules.isDisabled(ClipsModule.RULE_CLIPS)) {
            Notify.show(new Notify.Toast(Lang.tr("clips.toast.blocked"), ServerRules.message(), Notify.Kind.WARN, null, null));
            return;
        }
        if (!m.buffer.on()) {
            Notify.show(new Notify.Toast(Lang.tr("clips.toast.bufferOff"), Lang.tr("clips.toast.bufferOff.body"), Notify.Kind.INFO, null,
                    () -> ClipsPages.openSettings()));
            return;
        }
        if (!hasFootage()) {
            Notify.show(new Notify.Toast(Lang.tr("clips.toast.nothing"), Lang.tr("clips.toast.nothing.body"), Notify.Kind.INFO, null, null));
            return;
        }
        ClipSaver.save(m, ring, "hotkey", m.length.get() * 1000L, null, encoder, 350);
    }

    /** An automatic clip (AutoClips, after its post-roll). */
    void saveAuto(ClipSaver.AutoInfo info) {
        if (!m.autoActive() || !hasFootage()) return;
        ClipSaver.save(m, ring, info.reason(), m.length.get() * 1000L, info, encoder, 0);
    }

    /** Low-FPS detection: a 3 s rolling average, below the threshold for 3 s pauses, threshold + 10 for 5 s resumes. */
    static final class FpsWatch {
        private static final int TICKS = 60; // 3 s of ticks
        private final float[] samples = new float[TICKS];
        private int n, idx;
        private int frames;
        private long lastTick;
        private int belowTicks, aboveTicks;
        private boolean low;
        float average;

        void frame(long nanos) {
            frames++;
        }

        void reset() {
            n = 0;
            idx = 0;
            frames = 0;
            lastTick = 0;
            belowTicks = 0;
            aboveTicks = 0;
            low = false;
        }

        /** Called once per tick with the current threshold; returns whether recording should pause for low FPS. */
        boolean low(int threshold) {
            long now = System.nanoTime();
            if (lastTick != 0) {
                float dt = (now - lastTick) / 1e9f;
                if (dt > 0.001f) {
                    samples[idx] = frames / dt;
                    idx = (idx + 1) % TICKS;
                    if (n < TICKS) n++;
                }
            }
            frames = 0;
            lastTick = now;
            if (n < 20) return low; // under a second of data
            float sum = 0;
            for (int i = 0; i < n; i++) sum += samples[i];
            average = sum / n;
            if (!low) {
                belowTicks = average < threshold ? belowTicks + 1 : 0;
                if (belowTicks >= 60) {
                    low = true;
                    aboveTicks = 0;
                }
            } else {
                aboveTicks = average >= threshold + 10 ? aboveTicks + 1 : 0;
                if (aboveTicks >= 100) {
                    low = false;
                    belowTicks = 0;
                }
            }
            return low;
        }
    }
}
