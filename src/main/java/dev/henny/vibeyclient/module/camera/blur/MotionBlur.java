package dev.henny.vibeyclient.module.camera.blur;

//? if >=1.21.11 && <26.1 {
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import dev.henny.vibeyclient.module.camera.CameraModule;
import dev.henny.vibeyclient.module.camera.core.BlurMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class MotionBlur {
    public static final boolean SUPPORTED = true;
    public static final Identifier ID = Identifier.fromNamespaceAndPath("vibeyclient", "motion_blur");

    private static boolean unavailable;
    private static boolean fresh = true;
    private static long lastNanos;
    private static Vec3 lastCamera;
    private static int width, height;
    private static float lastWeight;
    private static long frames;

    private MotionBlur() {}

    public static void apply(Minecraft mc, GraphicsResourceAllocator pool) {
        CameraModule m = CameraModule.get();
        if (m == null || !m.blurActive() || mc.level == null) {
            fresh = true;
            return;
        }
        PostChain chain = mc.getShaderManager().getPostChain(ID, LevelTargetBundle.MAIN_TARGETS);
        unavailable = chain == null;
        if (chain == null) return;
        RenderTarget main = mc.getMainRenderTarget();
        float weight = weight(m, mc, main);
        BlurUniform.write(chain, weight);
        chain.process(main, pool);
        lastWeight = weight;
        frames++;
    }

    public static boolean unavailable() {
        return unavailable;
    }

    public static float lastWeight() {
        return lastWeight;
    }

    public static long frames() {
        return frames;
    }

    public static boolean strengthFixed() {
        return BlurUniform.failed();
    }

    private static float weight(CameraModule m, Minecraft mc, RenderTarget main) {
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0 : (now - lastNanos) / 1e9;
        lastNanos = now;
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        boolean resized = main.width != width || main.height != height;
        boolean jumped = lastCamera == null
                || BlurMath.jumped(cam.x - lastCamera.x, cam.y - lastCamera.y, cam.z - lastCamera.z);
        boolean reset = fresh || resized || jumped;
        fresh = false;
        lastCamera = cam;
        width = main.width;
        height = main.height;
        return reset ? 0f : BlurMath.historyWeight(m.blurStrength.get(), dt);
    }
}
//?} else {
/*public final class MotionBlur {
    public static final boolean SUPPORTED = false;

    private MotionBlur() {}

    public static boolean unavailable() {
        return true;
    }

    public static float lastWeight() {
        return 0f;
    }

    public static long frames() {
        return 0;
    }

    public static boolean strengthFixed() {
        return false;
    }
}
*///?}
