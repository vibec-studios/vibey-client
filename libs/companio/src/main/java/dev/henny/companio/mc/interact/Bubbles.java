package dev.henny.companio.mc.interact;

import dev.henny.companio.world.CompanionView;
import dev.henny.r3.anchor.Facing;
import dev.henny.r3.interact.InteractionRules;
import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.mc.client.R3Client;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.text.Text3D;
import dev.henny.r3.text.TextStyle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

public final class Bubbles {
    private static final TextStyle BUBBLE = TextStyle.DEFAULT.withColor(0xFF1B1726).withBackground(0xF2FFFFFF, 2.5f);
    private static final TextStyle NAME = TextStyle.NAMETAG;

    private final Map<CompanionView, WorldObject> bubbles = new HashMap<>();
    private final Map<CompanionView, WorldObject> names = new HashMap<>();

    public void update(Iterable<CompanionView> views, Set<CompanionView> named, boolean enabled, BiPredicate<CompanionView, double[]> position) {
        if (!R3Client.started()) return;
        Map<CompanionView, Boolean> seen = new HashMap<>();
        for (CompanionView v : views) {
            seen.put(v, true);
            String text = enabled ? v.bubbleText() : "";
            show(bubbles, v, text, BUBBLE, 0.32f, position);
            String name = named.contains(v) ? v.name() : "";
            show(names, v, name, NAME, bubbleTextShown(v, enabled) ? 0.62f : 0.18f, position);
        }
        prune(bubbles, seen);
        prune(names, seen);
    }

    private static boolean bubbleTextShown(CompanionView v, boolean enabled) {
        return enabled && !v.bubbleText().isEmpty();
    }

    private static void show(Map<CompanionView, WorldObject> map, CompanionView v, String text, TextStyle style, float lift,
                             BiPredicate<CompanionView, double[]> position) {
        WorldObject o = map.get(v);
        if (text.isEmpty()) {
            if (o != null) o.visibility().hidden(true);
            return;
        }
        if (o == null) {
            Text3D t = new Text3D(text).style(style).facing(Facing.CAMERA).pixelSize(0.016f);
            o = WorldObject.create("companio:bubble").content(t).rules(new InteractionRules().interactive(false));
            o.dimension(R3Mc.scene().query().dimension());
            map.put(v, o);
            R3Mc.add(o);
        }
        float[] liftRef = {lift};
        o.anchoredTo(new FollowAnchor(p -> {
            if (!position.test(v, p)) return false;
            p[1] += v.body().height + liftRef[0] * Math.max(0.6f, v.size());
            return true;
        }));
        o.visibility().hidden(false);
        Text3D t = (Text3D) o.content();
        if (!t.text().equals(text)) t.text(text);
    }

    private static void prune(Map<CompanionView, WorldObject> map, Map<CompanionView, Boolean> seen) {
        for (Iterator<Map.Entry<CompanionView, WorldObject>> it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<CompanionView, WorldObject> e = it.next();
            if (seen.containsKey(e.getKey())) continue;
            R3Mc.remove(e.getValue());
            it.remove();
        }
    }

    public void clear() {
        if (R3Client.started()) {
            for (WorldObject o : bubbles.values()) R3Mc.remove(o);
            for (WorldObject o : names.values()) R3Mc.remove(o);
        }
        bubbles.clear();
        names.clear();
    }
}
