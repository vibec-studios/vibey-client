package dev.henny.cosmetico.mc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.cosmetico.mesh.MeshPart;
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
//?} else
/*import net.minecraft.client.renderer.RenderType;*/

public abstract class MeshSink {
    public static final int FULL_BRIGHT = 0xF000F0;
    public static final int NO_OVERLAY = 655360;

    public abstract void draw(PoseStack pose, RenderType type, MeshPart part, int light, int overlay, int argb);

    public abstract void quads(PoseStack pose, RenderType type, float[] data, int quads, int light, int argb);
}
