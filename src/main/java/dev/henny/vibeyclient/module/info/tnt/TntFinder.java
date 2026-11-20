package dev.henny.vibeyclient.module.info.tnt;

import dev.henny.vibeyclient.module.info.look.LookTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class TntFinder {
    public static final double REACH = 24;

    private TntFinder() {}

    public static PrimedTnt find(Minecraft mc) {
        if (LookTarget.entity() instanceof PrimedTnt t) return t;
        Vec3 eye = mc.player.getEyePosition();
        Vec3 end = eye.add(mc.player.getViewVector(1f).scale(REACH));
        HitResult block = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        double max = block.getType() == HitResult.Type.MISS ? REACH : block.getLocation().distanceTo(eye);
        PrimedTnt best = null;
        double bestSqr = max * max;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof PrimedTnt tnt) || e.distanceToSqr(eye) > (REACH + 2) * (REACH + 2)) continue;
            Optional<Vec3> hit = tnt.getBoundingBox().inflate(0.1).clip(eye, end);
            if (hit.isEmpty()) continue;
            double d = hit.get().distanceToSqr(eye);
            if (d < bestSqr) {
                bestSqr = d;
                best = tnt;
            }
        }
        return best;
    }
}
