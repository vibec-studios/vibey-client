package dev.henny.vibeyclient.module.camera.blur;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.module.camera.mixin.PostChainAccessor;
import dev.henny.vibeyclient.module.camera.mixin.PostPassAccessor;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

final class BlurUniform {
    static final String BLOCK = "MotionBlurConfig";
    private static final int SIZE = 16;
    private static final ByteBuffer DATA = ByteBuffer.allocateDirect(SIZE).order(ByteOrder.nativeOrder());

    private static PostChain patched;
    private static GpuBuffer buffer;
    private static boolean failed;

    private BlurUniform() {}

    static boolean write(PostChain chain, float weight) {
        if (failed) return false;
        try {
            if (chain != patched || buffer == null || buffer.isClosed()) patch(chain);
            if (buffer == null) return false;
            DATA.clear();
            DATA.putFloat(0, weight);
            DATA.position(0).limit(SIZE);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), DATA);
            return true;
        } catch (RuntimeException | LinkageError e) {
            failed = true;
            Vibey.LOG.warn("Motion blur: strength can't be changed, using the default ({})", e.toString());
            return false;
        }
    }

    static boolean failed() {
        return failed;
    }

    private static void patch(PostChain chain) {
        patched = chain;
        buffer = null;
        for (PostPass pass : ((PostChainAccessor) chain).vibey$passes()) {
            Map<String, GpuBuffer> uniforms = ((PostPassAccessor) pass).vibey$customUniforms();
            GpuBuffer old = uniforms.get(BLOCK);
            if (old == null) continue;
            if (buffer == null) buffer = RenderSystem.getDevice().createBuffer(() -> "vibeyclient motion blur",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, SIZE);
            uniforms.put(BLOCK, buffer);
            old.close();
        }
    }
}
