package dev.henny.vibeyclient.module.cinematic.run;

import dev.henny.vibeyclient.module.cinematic.core.plan.Scene;
import net.minecraft.client.multiplayer.ClientLevel;

final class Skies {
    private Skies() {}

    static Scene.Sky of(ClientLevel level) {
        if (!level.dimensionType().hasSkyLight()) return new Scene.Sky(0, false, false);
        long t = Math.floorMod(level.getDayTime(), 24000L);
        boolean morning = t >= 22500 || t <= 1500;
        boolean evening = t >= 10500 && t <= 13500;
        boolean night = t > 13000 && t < 23000;
        return new Scene.Sky(evening ? 90 : -90, morning || evening, night);
    }
}
