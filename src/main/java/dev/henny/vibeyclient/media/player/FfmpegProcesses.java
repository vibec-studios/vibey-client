package dev.henny.vibeyclient.media.player;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class FfmpegProcesses {
    private static final Set<Process> LIVE = ConcurrentHashMap.newKeySet();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> LIVE.forEach(Process::destroyForcibly), "Vibey video cleanup"));
    }

    private FfmpegProcesses() {}

    static Process start(List<String> cmd, boolean keepErrors) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (!keepErrors) pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process p = pb.start();
        LIVE.add(p);
        p.onExit().thenRun(() -> LIVE.remove(p));
        try {
            p.getOutputStream().close();
        } catch (IOException ignored) {}
        return p;
    }
}
