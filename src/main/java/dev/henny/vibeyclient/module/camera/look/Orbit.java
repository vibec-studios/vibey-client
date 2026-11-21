package dev.henny.vibeyclient.module.camera.look;

import com.mojang.blaze3d.platform.InputConstants;
import dev.henny.vibeyclient.module.camera.CameraModule;
import dev.henny.vibeyclient.module.camera.core.OrbitOffset;
import dev.henny.vibeyclient.module.camera.core.TapOrDrag;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class Orbit {
    public static final int MIDDLE = 2;
    private static final OrbitOffset OFFSET = new OrbitOffset();
    private static final TapOrDrag GESTURE = new TapOrDrag();

    private Orbit() {}

    static void update(CameraModule m, Minecraft mc) {
        boolean usable = m != null && m.orbitActive() && thirdPerson(mc);
        if (usable) return;
        GESTURE.cancel();
        if (!OFFSET.idle()) OFFSET.reset();
    }

    public static boolean press(Minecraft mc) {
        CameraModule m = CameraModule.get();
        if (m == null || !m.orbitActive() || !thirdPerson(mc) || mc.player == null) return false;
        if (mc.getCameraEntity() != mc.player || R3Busy.busy()) return false;
        GESTURE.press(System.currentTimeMillis());
        return true;
    }

    public static boolean release(Minecraft mc) {
        if (!GESTURE.down()) return false;
        if (GESTURE.release(System.currentTimeMillis()) && pickBlockOnMiddle(mc)) {
            KeyMapping.click(InputConstants.Type.MOUSE.getOrCreate(dev.henny.vibeyclient.client.Keys.mouseToNative(MIDDLE)));
        }
        return true;
    }

    public static boolean capturing() {
        return GESTURE.down();
    }

    public static boolean wheel(double notches) {
        if (!GESTURE.down()) return false;
        OFFSET.wheel(notches);
        return true;
    }

    static void drag(double dx, double dy, float basePitch) {
        GESTURE.move(dx, dy);
        OFFSET.drag(dx, dy, basePitch);
    }

    static boolean applies(Minecraft mc) {
        return !OFFSET.idle() && thirdPerson(mc);
    }

    static float yaw(float base) {
        return OFFSET.yaw(base);
    }

    static float pitch(float base) {
        return OFFSET.pitch(base);
    }

    public static float zoom() {
        return OFFSET.zoom();
    }

    public static float yawOffset() {
        return OFFSET.yawOffset();
    }

    public static float pitchOffset() {
        return OFFSET.pitchOffset();
    }

    static void reset() {
        GESTURE.cancel();
        OFFSET.reset();
    }

    private static boolean thirdPerson(Minecraft mc) {
        return !mc.options.getCameraType().isFirstPerson();
    }

    private static boolean pickBlockOnMiddle(Minecraft mc) {
        return "key.mouse.middle".equals(mc.options.keyPickItem.saveString());
    }
}
