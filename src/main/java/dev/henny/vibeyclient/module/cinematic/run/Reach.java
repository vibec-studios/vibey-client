package dev.henny.vibeyclient.module.cinematic.run;

import dev.henny.vibeyclient.module.cinematic.CinematicRules;
import net.minecraft.client.Minecraft;

final class Reach {
    private Reach() {}

    static int limit(Minecraft mc) {
        int chunks = mc.options.getEffectiveRenderDistance();
        return Math.max(8, Math.min(CinematicRules.MAX_RADIUS, (chunks - 1) * 16));
    }
}
