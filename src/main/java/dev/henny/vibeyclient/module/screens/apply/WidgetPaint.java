package dev.henny.vibeyclient.module.screens.apply;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.joml.Matrix3x2fStack;

public final class WidgetPaint {
    private WidgetPaint() {}

    public static void render(AbstractWidget w, WidgetState s, GuiGraphics g, int mx, int my, float dt, Operation<Void> original) {
        if (s.hidden) {
            w.visible = false;
            return;
        }
        float k = s.scale;
        int x = w.getX(), y = w.getY();
        int rw = w.getWidth(), rh = w.getHeight();
        StateHolder h = (StateHolder) w;
        Matrix3x2fStack pose = g.pose();
        h.vibey$size(s.logicalW, s.logicalH);
        pose.pushMatrix();
        try {
            pose.translate(x, y);
            pose.scale(k, k);
            pose.translate(-x, -y);
            original.call(g, x + (int) Math.floor((mx - x) / k), y + (int) Math.floor((my - y) / k), dt);
        } finally {
            pose.popMatrix();
            h.vibey$size(rw, rh);
        }
    }
}
