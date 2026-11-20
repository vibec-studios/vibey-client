package dev.henny.vibeyclient.module.info.world;

import dev.henny.r3.anchor.Anchor;
import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.text.Text3D;
import dev.henny.r3.text.TextStyle;

public final class WorldLabel {
    private static final TextStyle BASE = TextStyle.NAMETAG.withBackground(0x66000000, 1.5f);

    private final float pixelSize;
    private WorldObject object;
    private Text3D text;
    private int color;

    public WorldLabel(float pixelSize) {
        this.pixelSize = pixelSize;
    }

    public void show(String value, int argb, double x, double y, double z) {
        ensure();
        text.text(value);
        recolor(argb);
        object.anchor(null);
        object.transform().position(x, y, z);
        object.visibility().hidden(false);
    }

    public void follow(String value, int argb, Anchor anchor) {
        ensure();
        text.text(value);
        recolor(argb);
        if (object.anchor() != anchor) object.anchor(anchor);
        object.visibility().hidden(false);
    }

    public void hide() {
        if (object != null) object.visibility().hidden(true);
    }

    public boolean visible() {
        return object != null && !object.visibility().hidden() && R3Mc.scene().get(object.id()) != null;
    }

    public String value() {
        return text == null ? "" : text.text();
    }

    public void remove() {
        if (object != null) R3Mc.remove(object);
        object = null;
        text = null;
    }

    private void recolor(int argb) {
        if (argb == color) return;
        color = argb;
        text.style(BASE.withColor(argb));
    }

    private void ensure() {
        if (object != null && R3Mc.scene().get(object.id()) != null
                && java.util.Objects.equals(object.dimension(), R3Mc.scene().query().dimension())) return;
        if (object != null) R3Mc.remove(object);
        object = R3Mc.text("");
        text = (Text3D) object.content();
        text.pixelSize(pixelSize);
        color = 0;
        object.visibility().maxDistance(48);
        R3Mc.add(object);
    }
}
