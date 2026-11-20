package dev.henny.cosmetico.mc.fx;

import dev.henny.cosmetico.mc.CosmeticoMod;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

final class OggDecoder {
    private OggDecoder() {}

    record Pcm(short[] samples, int channels, int rate) {}

    static Pcm decode(byte[] ogg) {
        ByteBuffer data = MemoryUtil.memAlloc(ogg.length);
        long handle = 0;
        ShortBuffer chunk = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            data.put(ogg).flip();
            IntBuffer err = stack.mallocInt(1);
            handle = STBVorbis.stb_vorbis_open_memory(data, err, null);
            if (handle == 0) return null;
            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(handle, info);
            int channels = info.channels(), rate = info.sample_rate();
            chunk = MemoryUtil.memAllocShort(4096 * channels);
            return read(handle, chunk, channels, rate);
        } catch (Throwable t) {
            CosmeticoMod.LOG.warn("Could not decode embedded sound: {}", t.toString());
            return null;
        } finally {
            if (handle != 0) STBVorbis.stb_vorbis_close(handle);
            if (chunk != null) MemoryUtil.memFree(chunk);
            MemoryUtil.memFree(data);
        }
    }

    private static Pcm read(long handle, ShortBuffer chunk, int channels, int rate) {
        short[] out = new short[Math.max(4096, STBVorbis.stb_vorbis_stream_length_in_samples(handle) * channels)];
        int n = 0;
        while (true) {
            chunk.clear();
            int frames = STBVorbis.stb_vorbis_get_samples_short_interleaved(handle, channels, chunk);
            if (frames <= 0) break;
            int count = frames * channels;
            if (n + count > out.length) out = Arrays.copyOf(out, Math.max(out.length * 2, n + count));
            chunk.get(out, n, count);
            n += count;
            if (n > 30 * rate * channels) break;
        }
        return new Pcm(Arrays.copyOf(out, n), channels, rate);
    }
}
