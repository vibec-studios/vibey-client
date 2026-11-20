package dev.henny.vibeyclient.media.player;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

final class FfmpegVideoLoop implements Runnable {
    private final FfmpegSession s;

    FfmpegVideoLoop(FfmpegSession s) {
        this.s = s;
    }

    @Override
    public void run() {
        int frameBytes = s.spec.w() * s.spec.h() * 4;
        byte[] tmp = new byte[1 << 16];
        try (InputStream in = s.video.getInputStream()) {
            long index = 0;
            while (!s.closed) {
                Integer idx = s.free.poll(100, TimeUnit.MILLISECONDS);
                if (idx == null) continue;
                ByteBuffer b = s.ring[idx];
                b.clear();
                int got = 0;
                while (got < frameBytes) {
                    int n = in.read(tmp, 0, Math.min(tmp.length, frameBytes - got));
                    if (n < 0) break;
                    b.put(tmp, 0, n);
                    got += n;
                }
                if (got < frameBytes) {
                    s.free.offer(idx);
                    break;
                }
                s.pts[idx] = s.startMs + (long) (index * 1000.0 / s.spec.fps());
                index++;
                s.framesRead = index;
                s.ready.put(idx);
            }
        } catch (IOException | InterruptedException ignored) {
        } finally {
            s.videoEnded = true;
            try {
                s.videoExit = s.video.waitFor(2, TimeUnit.SECONDS) ? s.video.exitValue() : -1;
            } catch (InterruptedException e) {
                s.videoExit = -1;
            }
        }
    }
}
