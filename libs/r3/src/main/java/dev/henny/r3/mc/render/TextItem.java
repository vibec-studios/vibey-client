package dev.henny.r3.mc.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

final class TextItem {
    final Matrix4f pose = new Matrix4f();
    String text;
    float x;
    float y;
    int argb;
    int light;
    TextMode mode;
    boolean shadow;
    boolean bold;
    String font;

    TextItem set(Matrix4fc pose, String text, float x, float y, int argb, int light, TextMode mode, boolean shadow, boolean bold) {
        this.pose.set(pose);
        this.text = text;
        this.x = x;
        this.y = y;
        this.argb = argb;
        this.light = light;
        this.mode = mode;
        this.shadow = shadow;
        this.bold = bold;
        this.font = null;
        return this;
    }
}
