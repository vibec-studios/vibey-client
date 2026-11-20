package dev.henny.vibeyclient.module.camera.look;

import dev.henny.vibeyclient.module.camera.CameraModule;
import dev.henny.vibeyclient.module.camera.core.LookAngles;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public final class FreeLook {
    private static final LookAngles ANGLES = new LookAngles();
    private static boolean active;
    private static boolean toggled;
    private static boolean switched;
    private static CameraType before = CameraType.FIRST_PERSON;

    private FreeLook() {}

    public static void keyPressed() {
        CameraModule m = CameraModule.get();
        if (m != null && m.freelookActive() && m.freelookToggle.on()) toggled = !toggled;
    }

    static void update(CameraModule m, Minecraft mc) {
        boolean want = m != null && m.freelookActive() && mc.player != null && mc.getCameraEntity() == mc.player
                && (m.freelookToggle.on() ? toggled : m.freelookKey.isDown());
        if (want && !active) start(mc);
        else if (!want && active) stop(mc);
        if (m == null || !m.freelookToggle.on()) toggled = false;
    }

    public static boolean active() {
        return active;
    }

    public static float yaw() {
        return ANGLES.yaw();
    }

    public static float pitch() {
        return ANGLES.pitch();
    }

    static void turn(double dx, double dy) {
        ANGLES.turn(dx, dy);
    }

    static void reset(Minecraft mc) {
        if (active) stop(mc);
        toggled = false;
    }

    private static void start(Minecraft mc) {
        ANGLES.reset(mc.player.getYRot(), mc.player.getXRot());
        before = mc.options.getCameraType();
        switched = before.isFirstPerson();
        if (switched) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        active = true;
    }

    private static void stop(Minecraft mc) {
        active = false;
        toggled = false;
        if (switched && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK) mc.options.setCameraType(before);
        switched = false;
    }
}
