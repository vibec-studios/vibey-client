package dev.henny.vibeyclient.module.camera;

import dev.henny.vibeyclient.api.ServerRules;

import java.util.List;

public final class CameraRules {
    public static final String FREELOOK = "camera.freelook", LEGACY_FREELOOK = "freelook", ORBIT = "camera.orbit";
    public static final List<String> ALL = List.of(FREELOOK, ORBIT);

    private CameraRules() {}

    public static void declare() {
        for (String key : ALL) ServerRules.supports(key);
        ServerRules.supports(LEGACY_FREELOOK);
    }

    public static boolean freelookBlocked() {
        return ServerRules.isDisabled(FREELOOK) || ServerRules.isDisabled(LEGACY_FREELOOK);
    }

    public static boolean orbitBlocked() {
        return ServerRules.isDisabled(ORBIT);
    }
}
