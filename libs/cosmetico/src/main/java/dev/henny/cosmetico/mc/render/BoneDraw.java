package dev.henny.cosmetico.mc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.cosmetico.mesh.MeshPart;
import dev.henny.cosmetico.skeleton.BonePose;
import dev.henny.cosmetico.skeleton.LocatorSink;
import dev.henny.cosmetico.skeleton.Mix;
import dev.henny.cosmetico.skeleton.PartFrames;
import dev.henny.cosmetico.skeleton.Pass;
import dev.henny.cosmetico.skeleton.Rig;
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
//?} else
/*import net.minecraft.client.renderer.RenderType;*/

public final class BoneDraw {
    private final BonePose pose = new BonePose();

    public void draw(GpuBundle gb, Mix mix, PoseStack ps, MeshSink sink, PartFrames parts, Pass pass, int light, int color,
                     LocatorSink locators) {
        Rig rig = gb.rig;
        if (rig.bundle.model == null) return;
        pose.compute(rig, mix, ps.last().pose(), ps.last().normal(), parts, pass, locators);
        for (int i = 0; i < pose.count(); i++) {
            if (!pose.drawn(i) || !pose.visible(i)) continue;
            MeshPart[] list = rig.mesh.parts[i];
            if (list.length == 0) continue;
            ps.pushPose();
            ps.last().pose().set(pose.matrix(i));
            ps.last().normal().set(pose.normal(i));
            for (MeshPart part : list) part(gb, part, ps, sink, light, color);
            ps.popPose();
        }
    }

    private static void part(GpuBundle gb, MeshPart part, PoseStack ps, MeshSink sink, int light, int color) {
        Tex tex = gb.texture(part.texture);
        if (tex == null) return;
        RenderType type;
        int l = light;
        switch (part.mode) {
            case EMISSIVE -> {
                type = tex.emissive();
                l = MeshSink.FULL_BRIGHT;
            }
            case GLOW -> {
                type = tex.glow();
                l = MeshSink.FULL_BRIGHT;
            }
            case TRANSLUCENT -> type = tex.translucent();
            default -> type = tex.cutout();
        }
        sink.draw(ps, type, part, l, MeshSink.NO_OVERLAY, color);
    }
}
