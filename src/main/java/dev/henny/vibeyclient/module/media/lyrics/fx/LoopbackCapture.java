package dev.henny.vibeyclient.module.media.lyrics.fx;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.module.media.lyrics.LyricsFeature;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * What the PC plays (Spotify, a browser, ...), for the sky effects: WASAPI loopback of the default output device on
 * Windows, through a small helper ({@code assets/vibeyclient/lyrics/loopback/windows.ps1}: C# compiled by Windows
 * PowerShell's Add-Type). Loopback needs no permission and no driver ("Stereo Mix" is not needed), nothing is stored
 * or sent anywhere: the helper streams 12 kHz mono 16-bit PCM to the game over stdout and ends with the game.
 * Optional: the setting "Use PC sound", only while music from the PC plays; if the helper can't run (other OS,
 * locked-down PowerShell, no output device) the effects fall back to the lyrics timing.
 */
public final class LoopbackCapture {
    public enum State { OFF, STARTING, RUNNING, FAILED, UNSUPPORTED }

    private static final PcmRing RING = new PcmRing(1 << 14);
    private static volatile State state = State.OFF;
    private static volatile boolean wanted;
    private static volatile boolean pinned;
    private static volatile long unwantedSince;
    private static Thread thread;
    private static volatile Process process;
    private static int failures;

    private LoopbackCapture() {}

    public static State state() {
        return state;
    }

    public static PcmRing ring() {
        return RING;
    }

    /** Sound arrived in the last 300 ms (loopback sends nothing while the PC is silent). */
    public static boolean fresh() {
        return state == State.RUNNING && System.nanoTime() - RING.writeNanos() < 300_000_000L;
    }

    /** Client tick: whether the effects want the PC sound now; the helper stops 10 s after it is no longer wanted. */
    public static synchronized void want(boolean on) {
        if (pinned) on = true;
        if (on) {
            wanted = true;
            unwantedSince = 0;
            if (thread == null && state != State.UNSUPPORTED) {
                thread = new Thread(LoopbackCapture::loop, "Vibey Media loopback");
                thread.setDaemon(true);
                thread.start();
            }
            return;
        }
        if (wanted) {
            wanted = false;
            unwantedSince = System.nanoTime();
        }
        if (thread != null && unwantedSince != 0 && System.nanoTime() - unwantedSince > 10_000_000_000L) stopNow();
    }

    /** Tests: keep the helper wanted regardless of what plays (false = back to normal). */
    public static synchronized void pin(boolean on) {
        pinned = on;
        want(on);
    }

    /** The setting changed: try again after the helper was given up on. */
    public static synchronized void retry() {
        if (state == State.UNSUPPORTED) state = State.OFF;
        failures = 0;
    }

    public static synchronized void stopNow() {
        wanted = false;
        Process p = process;
        if (p != null) p.destroy();
        if (thread != null) thread.interrupt();
        thread = null;
        if (state != State.UNSUPPORTED) state = State.OFF;
    }

    private static void loop() {
        while (wanted || unwantedSince != 0) {
            if (!wanted) {
                if (System.nanoTime() - unwantedSince > 10_000_000_000L) break;
                sleep(200);
                continue;
            }
            long started = System.nanoTime();
            try {
                state = State.STARTING;
                run();
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                LyricsFeature.LOG.debug("PC sound helper: {}", e.toString());
            }
            if (Thread.currentThread().isInterrupted()) break;
            // a helper that ran a while and ended (device changed) restarts at once; quick failures back off
            if (System.nanoTime() - started > 20_000_000_000L) failures = 0;
            else failures++;
            if (failures >= 4) {
                state = State.UNSUPPORTED;
                LyricsFeature.LOG.info("PC sound for the sky effects is not available here; using the lyrics timing");
                break;
            }
            state = State.FAILED;
            if (!sleep(failures * 3000L)) break;
        }
        synchronized (LoopbackCapture.class) {
            if (thread == Thread.currentThread()) thread = null;
        }
        if (state != State.UNSUPPORTED) state = State.OFF;
    }

    private static void run() throws IOException, InterruptedException {
        Path dir = Vibey.platform().configDir().resolve("vibeyclient").resolve("media");
        Files.createDirectories(dir);
        Path script = dir.resolve("loopback.ps1");
        try (InputStream in = LoopbackCapture.class.getResourceAsStream("/assets/vibeyclient/lyrics/loopback/windows.ps1")) {
            if (in == null) throw new IOException("helper script missing");
            Files.copy(in, script, StandardCopyOption.REPLACE_EXISTING);
        }
        ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-File", script.toString(), String.valueOf(ProcessHandle.current().pid()));
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process p = pb.start();
        process = p;
        try (DataInputStream in = new DataInputStream(p.getInputStream())) {
            byte[] magic = new byte[4];
            in.readFully(magic);
            if (magic[0] != 'V' || magic[1] != 'M' || magic[2] != 'A' || magic[3] != '1') throw new IOException("bad helper output");
            int rate = Integer.reverseBytes(in.readInt());
            in.readInt(); // reserved
            if (rate < 4000 || rate > 96000) throw new IOException("bad rate " + rate);
            RING.reset(0, rate);
            state = State.RUNNING;
            failures = 0;
            LyricsFeature.LOG.info("Listening to the PC sound for the sky effects ({} Hz)", rate);
            byte[] buf = new byte[2048];
            while (wanted || (unwantedSince != 0 && System.nanoTime() - unwantedSince < 10_000_000_000L)) {
                int n = in.read(buf, 0, buf.length);
                if (n < 0) throw new EOFException("helper ended");
                if ((n & 1) == 1) {
                    in.readFully(buf, n, 1);
                    n++;
                }
                RING.writeLe16(buf, n / 2);
            }
        } finally {
            p.destroy();
            process = null;
        }
    }

    private static boolean sleep(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }
}
