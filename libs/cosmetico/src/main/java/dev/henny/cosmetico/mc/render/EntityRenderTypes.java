package dev.henny.cosmetico.mc.render;

//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?} else
/*import net.minecraft.client.renderer.RenderType;*/

public final class EntityRenderTypes {
    private EntityRenderTypes() {}

    static RenderType cutout(Tex t) {
        if (t.cutout == null) {
            //? if >=26.1 {
            /*t.cutout = RenderTypes.entityCutout(t.id);
            *///?} else if >=1.21.11 {
            t.cutout = RenderTypes.entityCutoutNoCull(t.id);
            //?} else
            /*t.cutout = RenderType.entityCutoutNoCull(t.id);*/
        }
        return t.cutout;
    }

    static RenderType translucent(Tex t) {
        if (t.translucent == null) {
            //? if >=1.21.11 && <26.2 {
            t.translucent = dev.henny.cosmetico.mc.mixin.RenderTypeInvoker.cosmetico$create("cosmetico_translucent",
                    net.minecraft.client.renderer.rendertype.RenderSetup.builder(SoftPipelines.translucent())
                            .withTexture("Sampler0", t.id).useLightmap().useOverlay().affectsCrumbling().sortOnUpload()
                            .setOutline(net.minecraft.client.renderer.rendertype.RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                            .createRenderSetup());
            //?} else if >=1.21.11 {
            /*t.translucent = RenderTypes.entityTranslucent(t.id);
            *///?} else
            /*t.translucent = RenderType.entityTranslucent(t.id);*/
        }
        return t.translucent;
    }

    static RenderType glow(Tex t) {
        if (t.glow == null) {
            //? if >=1.21.11 && <26.2 {
            t.glow = dev.henny.cosmetico.mc.mixin.RenderTypeInvoker.cosmetico$create("cosmetico_glow",
                    net.minecraft.client.renderer.rendertype.RenderSetup.builder(SoftPipelines.glow())
                            .withTexture("Sampler0", t.id).sortOnUpload().createRenderSetup());
            //?} else
            /*t.glow = emissive(t);*/
        }
        return t.glow;
    }

    static RenderType emissive(Tex t) {
        if (t.emissive == null) {
            //? if >=1.21.11 {
            t.emissive = RenderTypes.entityTranslucentEmissive(t.id);
            //?} else
            /*t.emissive = RenderType.entityTranslucentEmissive(t.id);*/
            //? if =1.21.5
            /*EMISSIVE.add(t.emissive);*/
        }
        return t.emissive;
    }

    //? if =1.21.5 {
    /*private static final java.util.Set<RenderType> EMISSIVE = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    static boolean isEmissive(RenderType type) {
        return EMISSIVE.contains(type);
    }
    *///?}
}
