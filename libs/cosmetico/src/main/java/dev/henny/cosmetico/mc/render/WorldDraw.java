package dev.henny.cosmetico.mc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.Engine;
import dev.henny.cosmetico.anim.EmotePlay;
import dev.henny.cosmetico.draw.CosmeticMix;
import dev.henny.cosmetico.draw.ShownBundle;
import dev.henny.cosmetico.mc.compat.PoseCompat;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.skeleton.Mix;
import dev.henny.cosmetico.skeleton.Pass;
import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.state.PetMotion;
import org.joml.Quaternionf;

public final class WorldDraw {
    private final BoneDraw bones = new BoneDraw();
    private final CosmeticMix mixes;
    private final Mix emoteMix = new Mix();
    private final Quaternionf q = new Quaternionf();
    private final LocatorCollector locators;

    public WorldDraw(LocatorCollector locators) {
        this.locators = locators;
        this.mixes = Cosmetico.engine().cosmeticMix();
    }

    public void draw(Avatar a, PoseStack ps, MeshSink sink, int light, double px, double py, double pz, float playerScale) {
        if (a.frame.hidden) return;
        Engine engine = Cosmetico.engine();
        double now = engine.clock.now();
        locators.target(a.preview ? null : a);
        for (Slot slot : Slot.all()) {
            if (slot != Slot.SHIELD) slot(engine.shown, a, slot, ps, sink, light, px, py, pz, playerScale, now);
        }
        emote(a, ps, sink, light, playerScale);
        locators.target(null);
    }

    private void slot(ShownBundle shown, Avatar a, Slot slot, PoseStack ps, MeshSink sink, int light, double px, double py,
                      double pz, float playerScale, double now) {
        Bundle b = shown.bundle(a, slot);
        if (b == null || b.model == null || !ShownBundle.hasWorldBones(b) || shown.hiddenByArmor(a, b)) return;
        if (slot == Slot.PET && ShownBundle.shoulder(b) != 0) return;
        GpuBundle gb = GpuCache.get(b);
        if (gb == null) return;
        ps.pushPose();
        if (slot == Slot.PET) petFrame(a, b, ps, px, py, pz, playerScale);
        else bodyFrame(a, ps, playerScale);
        WorldFrame.toModelSpace(ps);
        bones.draw(gb, mixes.of(a, b, slot, now), ps, sink, null, Pass.WORLD, light, -1, locators);
        ps.popPose();
    }

    private void petFrame(Avatar a, Bundle b, PoseStack ps, double px, double py, double pz, float playerScale) {
        PetMotion pm = a.pet;
        float pt = a.frame.partial;
        double x = WorldFrame.lerp(pt, pm.prevX, pm.x) - px, y = WorldFrame.lerp(pt, pm.prevY, pm.y) - py, z = WorldFrame.lerp(pt, pm.prevZ, pm.z) - pz;
        if (!pm.init) x = y = z = 0;
        ps.translate((float) x, (float) y, (float) z);
        float yaw = WorldFrame.lerpDeg(pt, pm.prevYaw, pm.yaw);
        PoseCompat.rotate(ps, q.rotationY((float) Math.toRadians(180f - yaw)));
        float s = b.pet == null ? 1f : b.pet.scale();
        ps.scale(s * playerScale, s * playerScale, s * playerScale);
    }

    private void bodyFrame(Avatar a, PoseStack ps, float playerScale) {
        PoseCompat.rotate(ps, q.rotationY((float) Math.toRadians(180f - a.frame.bodyYaw)));
        ps.scale(playerScale, playerScale, playerScale);
    }

    private void emote(Avatar a, PoseStack ps, MeshSink sink, int light, float playerScale) {
        EmotePlay e = a.emote;
        if (e == null || e.bundle.model == null || !ShownBundle.hasWorldBones(e.bundle)) return;
        GpuBundle gb = GpuCache.get(e.bundle);
        if (gb == null) return;
        ps.pushPose();
        bodyFrame(a, ps, playerScale);
        WorldFrame.toModelSpace(ps);
        bones.draw(gb, emoteMix.single(e.anim, e.localTime), ps, sink, null, Pass.WORLD, light, -1, locators);
        ps.popPose();
    }
}
