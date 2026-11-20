package dev.henny.vibeyclient.gametest;

import dev.henny.r3.object.Transform;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.vui.VuiPanelContent;
import dev.henny.vibeyclient.module.clips.shots.Shots;
import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import org.joml.Vector3d;

import java.util.List;

final class GalleryAim {
    private GalleryAim() {}

    static Vector3d panelPoint(double u, double v) {
        WorldObject panel = Shots.panel();
        Transform t = panel.transform();
        Vector3d right = t.orientation().transform(new Vector3d(1, 0, 0));
        Vector3d up = t.orientation().transform(new Vector3d(0, 1, 0));
        return new Vector3d(t.position()).add(right.mul((u - 0.5) * t.worldWidth())).add(up.mul((0.5 - v) * t.worldHeight()));
    }

    static Vector3d panelElement(String selector, int index) {
        WorldObject panel = Shots.panel();
        Document doc = ((VuiPanelContent) panel.content()).document();
        Element e = element(doc, selector, index);
        double u = (e.box().x + e.box().width / 2.0) / panel.pixelWidth();
        double v = (e.box().y + e.box().height / 2.0) / panel.pixelHeight();
        return panelPoint(u, v);
    }

    static void clickScreen(ClientGameTestContext ctx, String selector, int index) {
        double[] p = ctx.computeOnClient(mc -> {
            Document doc = Shots.screen().document();
            Element e = element(doc, selector, index);
            double scale = Minecraft.getInstance().getWindow().getGuiScale();
            return new double[] {(e.box().x + e.box().width / 2.0) * scale, (e.box().y + e.box().height / 2.0) * scale};
        });
        ctx.getInput().setCursorPos(p[0], p[1]);
        ctx.waitTicks(2);
        ctx.getInput().pressMouse(ReviewKit.LEFT);
        ctx.waitTicks(3);
    }

    private static Element element(Document doc, String selector, int index) {
        if (selector.startsWith("#")) {
            Element e = doc.find(selector.substring(1));
            if (e == null) throw new AssertionError("no element " + selector);
            return e;
        }
        List<Element> all = doc.query(selector);
        if (all.size() <= index) throw new AssertionError("only " + all.size() + " elements for " + selector);
        return all.get(index);
    }
}
