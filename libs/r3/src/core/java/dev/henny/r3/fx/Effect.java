package dev.henny.r3.fx;

import dev.henny.r3.object.ObjectContent;
import dev.henny.r3.render.WorldCanvas;

public interface Effect extends ObjectContent {
    void simulate(float dt, EffectContext context);

    void draw(WorldCanvas canvas, EffectContext context);

    default boolean finished() {
        return false;
    }

    default int liveParticles() {
        return 0;
    }
}
