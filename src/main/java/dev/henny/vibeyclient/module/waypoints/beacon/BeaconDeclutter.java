package dev.henny.vibeyclient.module.waypoints.beacon;

import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.module.waypoints.core.LabelStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

final class BeaconDeclutter {
    private BeaconDeclutter() {}

    static double pixelAngle(Minecraft mc) {
        double fov = Math.toRadians(mc.options.fov().get());
        int guiH = Math.max(1, mc.getWindow().getGuiScaledHeight());
        return 2 * Math.tan(fov / 2) / guiH;
    }

    static void apply(Minecraft mc, Collection<WorldObject> objects, double pixelAngle) {
        Player p = mc.player;
        if (p == null) return;
        List<LabelStack.Label> labels = new ArrayList<>();
        for (WorldObject o : objects) {
            if (o.visibility().hidden() || ((BeaconEffect) o.content()).shownPos() == null) continue;
            LabelStack.Label l = label(mc, p, o, pixelAngle);
            if (l != null) labels.add(l);
        }
        Map<String, Integer> levels = LabelStack.levels(labels);
        for (WorldObject o : objects) ((BeaconEffect) o.content()).labelLevel(levels.getOrDefault(o.id(), 0));
    }

    private static LabelStack.Label label(Minecraft mc, Player p, WorldObject o, double pixelAngle) {
        BeaconEffect e = (BeaconEffect) o.content();
        Vector3dc at = o.transform().position();
        double dx = at.x() - p.getX(), dy = at.y() - p.getEyeY(), dz = at.z() - p.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        double d = Math.sqrt(flat * flat + dy * dy);
        if (d < 0.5) return null;
        boolean hover = e.hovered();
        float size = o.transform().worldWidth();
        float px = BeaconLabel.scale((float) d, pixelAngle, size, hover);
        double lift = BeaconMarker.top(size, hover) + BeaconLabel.GAP * size;
        double base = Math.atan2(dy + lift, flat);
        double line = BeaconLabel.LINE * px / d;
        double width = (mc.font.width(e.labels().title()) + 4) * px / d;
        int lines = !hover ? 1 : e.labels().portal().isEmpty() ? 2 : 3;
        return new LabelStack.Label(o.id(), Math.atan2(dz, dx), base, width, line, lines, d);
    }
}
