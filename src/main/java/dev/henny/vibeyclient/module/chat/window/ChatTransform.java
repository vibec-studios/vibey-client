package dev.henny.vibeyclient.module.chat.window;

import org.joml.Matrix3x2f;

public final class ChatTransform {
    public float x;
    public float y;
    public float scale = 1f;
    public float anchorY;

    public boolean identity() {
        return x == 0 && y == anchorY && scale == 1f;
    }

    public void apply(Matrix3x2f m) {
        m.translate(x, y);
        m.scale(scale, scale);
        m.translate(0, -anchorY);
    }

    public double localX(double screenX) {
        return (screenX - x) / scale;
    }

    public double localY(double screenY) {
        return (screenY - y) / scale + anchorY;
    }
}
