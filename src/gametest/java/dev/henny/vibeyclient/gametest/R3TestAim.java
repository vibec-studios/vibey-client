package dev.henny.vibeyclient.gametest;

import dev.henny.r3.mc.demo.R3Demo;
import dev.henny.r3.object.Transform;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.vui.VuiPanelContent;
import dev.henny.vui.dom.Element;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import org.joml.Vector3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class R3TestAim {
    private R3TestAim() {}

    static Vector3d panelPoint(double u, double v) {
        WorldObject panel = R3Demo.panel();
        Transform t = panel.transform();
        Vector3d right = t.orientation().transform(new Vector3d(1, 0, 0));
        Vector3d up = t.orientation().transform(new Vector3d(0, 1, 0));
        return new Vector3d(t.position()).add(right.mul((u - 0.5) * t.worldWidth())).add(up.mul((0.5 - v) * t.worldHeight()));
    }

    static Vector3d control(String id) {
        WorldObject panel = R3Demo.panel();
        Element e = ((VuiPanelContent) panel.content()).document().find(id);
        double u = (e.box().x + e.box().width / 2.0) / panel.pixelWidth();
        double v = (e.box().y + e.box().height / 2.0) / panel.pixelHeight();
        return panelPoint(u, v);
    }

    static Vector3d edge(double outside) {
        Transform t = R3Demo.panel().transform();
        Vector3d right = t.orientation().transform(new Vector3d(1, 0, 0));
        return new Vector3d(t.position()).add(right.mul(t.worldWidth() / 2.0 + outside));
    }

    static Vector3d oppositeEdge() {
        Transform t = R3Demo.panel().transform();
        Vector3d right = t.orientation().transform(new Vector3d(1, 0, 0));
        return new Vector3d(t.position()).sub(right.mul(t.worldWidth() / 2.0));
    }

    static void lookAt(ClientGameTestContext ctx, Vector3d target) {
        float[] r = ctx.computeOnClient(mc -> {
            var e = mc.player.getEyePosition();
            double dx = target.x - e.x, dy = target.y - e.y, dz = target.z - e.z;
            return new float[] {(float) Math.toDegrees(Math.atan2(-dx, dz)), (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))};
        });
        look(ctx, r[0], r[1]);
    }

    static void look(ClientGameTestContext ctx, float yaw, float pitch) {
        ctx.runOnClient(mc -> {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
            mc.player.yRotO = yaw;
            mc.player.xRotO = pitch;
            mc.player.setYHeadRot(yaw);
        });
        ctx.waitTicks(4);
    }

    static void turn(ClientGameTestContext ctx, Vector3d from, Vector3d to, int steps) {
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            lookAt(ctx, new Vector3d(from).lerp(to, t));
        }
    }

    static void shot(ClientGameTestContext ctx, String name) {
        ctx.runOnClient(mc -> mc.gui.getChat().clearMessages(false));
        ctx.waitTicks(2);
        Path file = ctx.takeScreenshot("r3_" + name);
        Path review = root().resolve("build/review/r3");
        try {
            Files.createDirectories(review);
            Files.copy(file, review.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AssertionError("copy screenshot " + name, e);
        }
    }

    private static Path root() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("stonecutter.gradle"))) p = p.getParent();
        return p == null ? Path.of("").toAbsolutePath() : p;
    }
}
