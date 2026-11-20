package dev.henny.vibeyclient.ui.vui.corner;

import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.VibeyButton;
import net.minecraft.network.chat.Component;

final class ProxyButton extends VibeyButton {
    final String id;

    ProxyButton(String id, String label, Runnable action) {
        super(0, 0, 20, 20, Component.literal(label), null, action);
        this.id = id;
    }

    void place(float x, float y, float w, float h) {
        setX(Math.round(x));
        setY(Math.round(y));
        setWidth(Math.max(1, Math.round(w)));
        setHeight(Math.max(1, Math.round(h)));
    }

    @Override
    protected void draw(Gfx g, int mouseX, int mouseY) {
    }
}
