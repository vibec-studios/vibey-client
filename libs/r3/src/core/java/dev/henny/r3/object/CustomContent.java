package dev.henny.r3.object;

import dev.henny.r3.render.WorldCanvas;

public interface CustomContent extends ObjectContent {
    void draw(WorldCanvas canvas, WorldObject object, int light);
}
