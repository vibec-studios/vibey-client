package dev.henny.vibeyclient.module.clips.capture;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.module.clips.encode.EncodeSession;
import net.minecraft.resources.Identifier;

import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

/**
 * Render-thread capture, the way vanilla's {@code TracyFrameCapture} does it: right after the frame is blitted to the
 * screen, one fullscreen pass scales the main target into the clip size and converts it to NV12 on the GPU
 * ({@code clips_nv12.fsh}), then {@code copyTextureToBuffer} starts an asynchronous readback (GL: PBO + fence) into
 * one of three staging buffers. A buffer whose fence has passed is mapped a frame or more later, copied into a frame
 * of the {@link EncodeSession} and unmapped. Nothing here waits for the GPU. Frames are only captured when due
 * (pacing at the clip fps), so at 144 game fps and a 30 fps clip four frames in five do no work at all.
 */
public final class FrameGrabber implements AutoCloseable {
    private static final int SLOTS = 3;
    private static final int COPY_READ_BUFFER = 0x8F36;

    /**
     * How a finished readback reaches the CPU. MAP: map and copy on the render thread. GET: {@code glGetBufferSubData}
     * into our own memory (the driver's copy). ASYNC: map on the render thread, copy on a background thread.
     * (A GPU copy into an own {@code GL_STREAM_READ} buffer was tried too: just as slow to read on this PC.)
     */
    public enum Readback { MAP, GET, ASYNC }

    public static volatile Readback mode = Readback.valueOf(System.getProperty("vibeyclient.clips.readback", "ASYNC"));
    private static final java.util.concurrent.ExecutorService COPIER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Vibey clips copy");
        t.setDaemon(true);
        return t;
    });
    private final GpuBuffer.MappedView[] views = new GpuBuffer.MappedView[SLOTS];
    private final java.util.concurrent.atomic.AtomicIntegerArray copied = new java.util.concurrent.atomic.AtomicIntegerArray(SLOTS);
    private ByteBuffer staging;

    public volatile double copyMsAvg;

    private final int width, height;
    private final RenderPipeline pipeline;
    private GpuTexture target;
    private GpuTextureView targetView;
    private final GpuBuffer[] buffers = new GpuBuffer[SLOTS];
    private final boolean[] busy = new boolean[SLOTS];
    private final boolean[] done = new boolean[SLOTS];
    private final long[] capturedAt = new long[SLOTS];
    private boolean broken;
    // stats (render thread)
    public long captured, skippedNoSlot, skippedNoFrame, mapped;
    public long lastLatencyFrames;
    public double renderMsAvg, renderMsMax, mapMsAvg, issueMsAvg;
    private long frameCounter;
    private final long[] capturedFrame = new long[SLOTS];

    public FrameGrabber(int width, int height) {
        this.width = width;
        this.height = height;
        this.pipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("vibeyclient", "pipeline/clips_nv12_" + width + "x" + height))
                .withVertexShader("core/screenquad")
                .withFragmentShader(Identifier.fromNamespaceAndPath("vibeyclient", "core/clips_nv12"))
                .withShaderDefine("OUT_W", width)
                .withShaderDefine("OUT_H", height)
                .withSampler("InSampler")
                .withoutBlend()
                .withDepthWrite(false)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
                .build();
        GpuDevice device = RenderSystem.getDevice();
        target = device.createTexture(() -> "Vibey clips NV12", GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC,
                TextureFormat.RGBA8, width / 4, height * 3 / 2, 1, 1);
        targetView = device.createTextureView(target);
        long bytes = (long) width * height * 3 / 2;
        for (int i = 0; i < SLOTS; i++) {
            // system memory: the CPU reads it back
            buffers[i] = device.createBuffer(() -> "Vibey clips readback",
                    GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_HINT_CLIENT_STORAGE, bytes);
        }
    }

    /** Starts the averages afresh (measurements). */
    public void resetStats() {
        renderMsAvg = renderMsMax = mapMsAvg = issueMsAvg = copyMsAvg = 0;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean broken() {
        return broken;
    }

    /** Frames whose readback is still on its way. */
    public int inFlight() {
        int n = 0;
        for (boolean b : busy) if (b) n++;
        return n;
    }

    /**
     * Once per rendered frame after {@code blitToScreen}. Hands finished readbacks to {@code session} (may be null:
     * they are dropped) and, when {@code captureNow}, starts the next capture.
     */
    public void frame(RenderTarget main, EncodeSession session, boolean captureNow) {
        if (broken) return;
        frameCounter++;
        long t0 = System.nanoTime();
        boolean work = false;
        try {
            CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
            for (int i = 0; i < SLOTS; i++) {
                if (!busy[i] || !done[i] || views[i] != null) continue;
                work = true;
                busy[i] = false;
                done[i] = false;
                lastLatencyFrames = frameCounter - capturedFrame[i];
                if (session == null) continue;
                long m0 = System.nanoTime();
                Readback how = mode;
                if (how == Readback.ASYNC) {
                    // map now, copy on the copier thread; the slot stays taken until the copy is done
                    GpuBuffer.MappedView view = enc.mapBuffer(buffers[i], true, false);
                    views[i] = view;
                    busy[i] = true;
                    copied.set(i, 0);
                    final int s = i;
                    final EncodeSession target = session;
                    COPIER.execute(() -> {
                        try {
                            byte[] frame = target.borrow();
                            if (frame == null) {
                                skippedNoFrame++;
                                return;
                            }
                            long c0 = System.nanoTime();
                            ByteBuffer data = view.data();
                            data.get(data.position(), frame, 0, Math.min(frame.length, data.remaining()));
                            double cm = (System.nanoTime() - c0) / 1e6;
                            copyMsAvg = copyMsAvg == 0 ? cm : copyMsAvg * 0.95 + cm * 0.05;
                            target.submit(frame);
                        } finally {
                            copied.set(s, 1);
                        }
                    });
                } else {
                    byte[] frame = session.borrow();
                    if (frame == null) {
                        skippedNoFrame++;
                        continue;
                    }
                    if (how == Readback.GET) {
                        if (staging == null || staging.capacity() < frame.length) staging = org.lwjgl.system.MemoryUtil.memAlloc(frame.length);
                        int handle = ((dev.henny.vibeyclient.module.clips.mixin.GlBufferAccessor) buffers[i]).vibey$handle();
                        staging.clear().limit(frame.length);
                        org.lwjgl.opengl.GL15.glBindBuffer(COPY_READ_BUFFER, handle);
                        org.lwjgl.opengl.GL15.glGetBufferSubData(COPY_READ_BUFFER, 0L, staging);
                        org.lwjgl.opengl.GL15.glBindBuffer(COPY_READ_BUFFER, 0);
                        staging.get(0, frame, 0, frame.length);
                    } else {
                        try (GpuBuffer.MappedView view = enc.mapBuffer(buffers[i], true, false)) {
                            ByteBuffer data = view.data();
                            data.get(data.position(), frame, 0, Math.min(frame.length, data.remaining()));
                        }
                    }
                    session.submit(frame);
                }
                double mm = (System.nanoTime() - m0) / 1e6;
                mapMsAvg = mapMsAvg == 0 ? mm : mapMsAvg * 0.95 + mm * 0.05;
                mapped++;
            }
            // async copies that finished: unmap on the render thread, slot free again
            for (int i = 0; i < SLOTS; i++) {
                if (views[i] != null && copied.get(i) == 1) {
                    views[i].close();
                    views[i] = null;
                    busy[i] = false;
                }
            }
            if (captureNow && main.getColorTexture() != null) {
                int slot = -1;
                for (int i = 0; i < SLOTS; i++) {
                    if (!busy[i]) {
                        slot = i;
                        break;
                    }
                }
                if (slot < 0) {
                    skippedNoSlot++;
                } else {
                    work = true;
                    long i0 = System.nanoTime();
                    try (RenderPass pass = enc.createRenderPass(() -> "Vibey clips", targetView, OptionalInt.empty())) {
                        pass.setPipeline(pipeline);
                        pass.bindTexture("InSampler", main.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                        pass.draw(0, 3);
                    }
                    final int s = slot;
                    busy[s] = true;
                    done[s] = false;
                    capturedAt[s] = t0;
                    capturedFrame[s] = frameCounter;
                    enc.copyTextureToBuffer(target, buffers[s], 0L, () -> done[s] = true, 0);
                    captured++;
                    double im = (System.nanoTime() - i0) / 1e6;
                    issueMsAvg = issueMsAvg == 0 ? im : issueMsAvg * 0.95 + im * 0.05;
                }
            }
        } catch (RuntimeException e) {
            broken = true;
            Vibey.LOG.error("Clips: capture failed, recording stops", e);
            return;
        }
        if (work) {
            double ms = (System.nanoTime() - t0) / 1e6;
            renderMsAvg = renderMsAvg == 0 ? ms : renderMsAvg * 0.95 + ms * 0.05;
            if (ms > renderMsMax) renderMsMax = ms;
        }
    }

    /** Drops readbacks still in flight (pause, session change): their results are ignored. */
    public void forget() {
        // a pending callback only sets done[]; the slot is reused once it fired
        for (int i = 0; i < SLOTS; i++) {
            if (busy[i] && done[i]) {
                busy[i] = false;
                done[i] = false;
            }
        }
    }

    @Override
    public void close() {
        for (int i = 0; i < SLOTS; i++) {
            if (views[i] != null) {
                // a copy may still run: wait briefly so the memory stays valid
                for (int w = 0; w < 50 && copied.get(i) == 0; w++) java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);
                try {
                    views[i].close();
                } catch (RuntimeException ignored) {
                    // buffer gone
                }
                views[i] = null;
            }
        }
        if (staging != null) {
            org.lwjgl.system.MemoryUtil.memFree(staging);
            staging = null;
        }
        try {
            if (targetView != null) targetView.close();
            if (target != null) target.close();
            for (GpuBuffer b : buffers) if (b != null) b.close();
        } catch (RuntimeException e) {
            Vibey.LOG.debug("clips: closing capture buffers: {}", e.toString());
        }
        targetView = null;
        target = null;
    }
}
