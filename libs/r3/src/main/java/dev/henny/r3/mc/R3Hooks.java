package dev.henny.r3.mc;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.r3.mc.client.R3Client;
import dev.henny.r3.mc.render.McWorldCanvas;
//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
//?} else
/*import net.minecraft.client.renderer.MultiBufferSource;*/

public final class R3Hooks {
    private static final HookErrors errors = new HookErrors();

    private R3Hooks() {}

    public static boolean mouseButton(int nativeButton, int action) {
        if (!R3Client.started()) return false;
        try {
            return R3Client.get().input().nativeMouseButton(nativeButton, action);
        } catch (RuntimeException e) {
            errors.report("mouse", e);
            return false;
        }
    }

    public static boolean scroll(double notches) {
        if (!R3Client.started()) return false;
        try {
            return R3Client.get().input().scroll(notches);
        } catch (RuntimeException e) {
            errors.report("scroll", e);
            return false;
        }
    }

    public static boolean key(int nativeKey, int action) {
        if (!R3Client.started()) return false;
        try {
            return R3Client.get().input().nativeKey(nativeKey, action);
        } catch (RuntimeException e) {
            errors.report("key", e);
            return false;
        }
    }

    public static boolean character(int codepoint) {
        if (!R3Client.started()) return false;
        try {
            return R3Client.get().input().character(codepoint);
        } catch (RuntimeException e) {
            errors.report("char", e);
            return false;
        }
    }

    public static boolean hidesBlockOutline() {
        return R3Client.started() && R3Client.get().hidesBlockOutline();
    }

    //? if >=1.21.9 {
    public static void submitWorld(PoseStack pose, SubmitNodeCollector out) {
        McWorldCanvas canvas = prepare(pose);
        if (canvas != null) canvas.submit(out);
    }
    //?} else {
    /*public static void drawWorld(PoseStack pose, MultiBufferSource buffers) {
        if (buffers == null) return;
        McWorldCanvas canvas = prepare(pose);
        if (canvas != null) canvas.draw(buffers);
    }
    *///?}

    private static McWorldCanvas prepare(PoseStack pose) {
        if (!R3Client.started()) return null;
        try {
            return R3Client.get().renderWorld((pose == null ? new PoseStack() : pose).last().pose());
        } catch (RuntimeException e) {
            errors.report("render", e);
            return null;
        }
    }

    public static void startTick() {
        try {
            R3Mod.startClient();
            R3Client.get().startTick();
        } catch (RuntimeException e) {
            errors.report("tick", e);
        }
    }

    public static void endTick() {
        if (!R3Client.started()) return;
        try {
            R3Client.get().endTick();
        } catch (RuntimeException e) {
            errors.report("tick", e);
        }
    }
}
