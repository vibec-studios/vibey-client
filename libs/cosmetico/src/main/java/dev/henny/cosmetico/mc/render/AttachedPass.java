package dev.henny.cosmetico.mc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.Engine;
import dev.henny.cosmetico.anim.EmotePlay;
import dev.henny.cosmetico.draw.CosmeticMix;
import dev.henny.cosmetico.draw.ShownBundle;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.PlayerBone;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.skeleton.Mix;
import dev.henny.cosmetico.skeleton.Pass;
import dev.henny.cosmetico.state.Avatar;

public final class AttachedPass {
    private final BoneDraw bones = new BoneDraw();
    private final CosmeticMix mixes;
    private final Mix emoteMix = new Mix();
    private final LocatorCollector locators;

    public AttachedPass(LocatorCollector locators) {
        this.locators = locators;
        this.mixes = Cosmetico.engine().cosmeticMix();
    }

    public void draw(Avatar a, PoseStack ps, MeshSink sink, ModelPartFrames parts, int light) {
        if (a.frame.hidden) return;
        Engine engine = Cosmetico.engine();
        double now = engine.clock.now();
        locators.target(a.preview || GuiDepth.inGui() ? null : a);
        for (Slot slot : Slot.all()) {
            if (slot != Slot.SHIELD) slot(engine.shown, a, slot, ps, sink, parts, light, now);
        }
        emote(a.emote, ps, sink, parts, light);
        locators.target(null);
    }

    private void slot(ShownBundle shown, Avatar a, Slot slot, PoseStack ps, MeshSink sink, ModelPartFrames parts, int light, double now) {
        Bundle b = shown.bundle(a, slot);
        if (b == null || b.model == null || shown.hiddenByArmor(a, b)) return;
        GpuBundle gb = GpuCache.get(b);
        if (gb == null) return;
        Mix mix = mixes.of(a, b, slot, now);
        if (slot == Slot.PET && ShownBundle.shoulder(b) != 0) shoulderPet(gb, b, mix, ps, sink, parts, light);
        bones.draw(gb, mix, ps, sink, parts, Pass.ATTACHED, light, -1, locators);
    }

    private void shoulderPet(GpuBundle gb, Bundle b, Mix mix, PoseStack ps, MeshSink sink, ModelPartFrames parts, int light) {
        ps.pushPose();
        parts.applyTo(PlayerBone.BODY, ps);
        ps.translate(ShownBundle.shoulder(b) * -6f / 16f, -0.01f, 0.5f / 16f);
        float s = b.pet == null ? 1f : b.pet.scale();
        ps.scale(s, s, s);
        ps.translate(0f, -1.5f, 0f);
        bones.draw(gb, mix, ps, sink, null, Pass.WORLD, light, -1, locators);
        ps.popPose();
    }

    private void emote(EmotePlay e, PoseStack ps, MeshSink sink, ModelPartFrames parts, int light) {
        if (e == null || e.bundle.model == null) return;
        GpuBundle gb = GpuCache.get(e.bundle);
        if (gb == null) return;
        bones.draw(gb, emoteMix.single(e.anim, e.localTime), ps, sink, parts, Pass.ATTACHED, light, -1, locators);
    }
}
