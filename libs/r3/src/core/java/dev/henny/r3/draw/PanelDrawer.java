package dev.henny.r3.draw;

import dev.henny.r3.object.ObjectState;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.panel.PanelContent;
import dev.henny.r3.render.PanelCanvas;
import dev.henny.r3.render.WorldCanvas;
import org.joml.Matrix4f;

public final class PanelDrawer {
    private final PanelMatrix panelMatrix = new PanelMatrix();
    private final Matrix4f matrix = new Matrix4f();

    public void draw(WorldCanvas canvas, WorldObject object, PanelContent panel, int light) {
        int pw = object.pixelWidth();
        int ph = object.pixelHeight();
        ObjectState state = object.state();
        if (state.needsLayout(pw, ph)) {
            panel.layout(pw, ph);
            state.laidOut(pw, ph);
        }
        panelMatrix.compute(object.transform(), pw, ph, canvas.cameraPosition(), matrix);
        canvas.push();
        canvas.multiply(matrix);
        PanelCanvas pc = canvas.beginPanel(pw, ph, light, state.alpha());
        panel.draw(pc);
        canvas.endPanel();
        canvas.pop();
    }
}
