package dev.henny.vibeyclient.module.info.tnt;

import dev.henny.r3.anchor.EntityAnchor;
import dev.henny.r3.mc.client.R3Client;
import dev.henny.vibeyclient.module.info.InfoModule;
import dev.henny.vibeyclient.module.info.core.FuseMath;
import dev.henny.vibeyclient.module.info.world.WorldLabel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.item.PrimedTnt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class TntTimers {
    public static final int MAX = 8;
    private static final double KEEP_DISTANCE = 48;
    private static final List<Timer> TIMERS = new ArrayList<>();

    private record Timer(PrimedTnt tnt, EntityAnchor anchor, WorldLabel label) {}

    private TntTimers() {}

    public static void tick(InfoModule m, Minecraft mc) {
        if (!m.tntActive()) {
            clear();
            return;
        }
        PrimedTnt looked = TntFinder.find(mc);
        if (looked != null && !tracked(looked) && TIMERS.size() < MAX) {
            TIMERS.add(new Timer(looked, EntityAnchor.aboveHead(looked.getUUID(), 0.3), new WorldLabel(0.035f)));
        }
        Iterator<Timer> it = TIMERS.iterator();
        while (it.hasNext()) {
            Timer t = it.next();
            if (t.tnt().isRemoved() || t.tnt().distanceTo(mc.player) > KEEP_DISTANCE) {
                t.label().remove();
                it.remove();
                continue;
            }
            float s = FuseMath.seconds(t.tnt().getFuse(), 0f);
            t.label().follow(FuseMath.label(s), FuseMath.color(s), t.anchor());
        }
    }

    private static boolean tracked(PrimedTnt tnt) {
        for (Timer t : TIMERS) if (t.tnt() == tnt) return true;
        return false;
    }

    public static int count() {
        return TIMERS.size();
    }

    public static String firstLabel() {
        return TIMERS.isEmpty() ? null : TIMERS.get(0).label().value();
    }

    public static void clear() {
        if (R3Client.started()) for (Timer t : TIMERS) t.label().remove();
        TIMERS.clear();
    }
}
