package dev.henny.vui.mc.dev;

import dev.henny.vui.mc.VuiMod;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class FrameCapture {
    private static final int MAX_WAIT_TICKS = 3;

    private final List<Pending> pending = new ArrayList<>();
    private final Executor encoder = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "vui-png-encoder");
        t.setDaemon(true);
        return t;
    });

    private static final class Pending {
        final CompletableFuture<byte[]> future = new CompletableFuture<>();
        final long frame;
        int ticks;

        Pending(long frame) {
            this.frame = frame;
        }
    }

    public synchronized CompletableFuture<byte[]> request(long frameNow) {
        Pending p = new Pending(frameNow);
        pending.add(p);
        return p.future;
    }

    public void service(long frameNow) {
        List<Pending> due = new ArrayList<>();
        synchronized (this) {
            for (Pending p : pending) if (frameNow > p.frame || ++p.ticks > MAX_WAIT_TICKS) due.add(p);
            pending.removeAll(due);
        }
        if (due.isEmpty()) return;
        try {
            FramePixels.grab(pixels -> CompletableFuture.supplyAsync(() -> png(pixels), encoder).whenComplete((png, error) -> {
                for (Pending p : due) {
                    if (error != null) p.future.completeExceptionally(error);
                    else p.future.complete(png);
                }
            }));
        } catch (RuntimeException e) {
            VuiMod.LOG.warn("vui: screenshot failed", e);
            for (Pending p : due) p.future.completeExceptionally(e);
        }
    }

    static byte[] png(FramePixels pixels) {
        BufferedImage image = new BufferedImage(pixels.width(), pixels.height(), BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, pixels.width(), pixels.height(), pixels.argb(), 0, pixels.width());
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
