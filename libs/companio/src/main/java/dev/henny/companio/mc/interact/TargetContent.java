package dev.henny.companio.mc.interact;

import dev.henny.r3.anchor.Facing;
import dev.henny.r3.object.CustomContent;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.panel.PointerContent;
import dev.henny.r3.render.WorldCanvas;

final class TargetContent implements CustomContent, PointerContent {
    static final String PET = "pet";

    @Override
    public void draw(WorldCanvas canvas, WorldObject object, int light) {}

    @Override
    public String controlAt(float x, float y) {
        return PET;
    }

    @Override
    public Facing facing() {
        return Facing.CAMERA_YAW;
    }

    @Override
    public boolean translucent() {
        return false;
    }
}
