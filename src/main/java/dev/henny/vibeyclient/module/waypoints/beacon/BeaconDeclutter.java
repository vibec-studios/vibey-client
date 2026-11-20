package dev.henny.vibeyclient.module.waypoints.beacon;

import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.module.waypoints.core.LabelStack;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

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
            BeaconEffect e = (BeaconEffect) o.content();
            Pos s = e.shownPos();
            if (s == null || o.visibility().hidden()) continue;
            double dx = s.x() + 0.5 - p.getX(), dy = s.y() + BeaconAnchor.LIFT - p.getEyeY(), dz = s.z() + 0.5 - p.getZ();
            double flat = Math.sqrt(dx * dx + dz * dz);
            double width = (mc.font.width(e.labels().title()) + 4) * BeaconLabel.GUI * pixelAngle;
            double height = BeaconLabel.LINE * BeaconLabel.GUI * pixelAngle;
            labels.add(new LabelStack.Label(o.id(), Math.atan2(dz, dx), Math.atan2(dy, flat), width, height,
                    Math.sqrt(flat * flat + dy * dy)));
        }
        Map<String, Integer> levels = LabelStack.levels(labels);
        for (WorldObject o : objects) ((BeaconEffect) o.content()).labelLevel(levels.getOrDefault(o.id(), 0));
    }
}
