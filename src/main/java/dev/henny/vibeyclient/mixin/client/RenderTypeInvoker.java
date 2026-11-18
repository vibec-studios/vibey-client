package dev.henny.vibeyclient.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.gen.Invoker;
//?} else
/*import net.minecraft.client.renderer.RenderType;*/

/**
 * 1.21.11+: {@code RenderType.create(String, RenderSetup)} is package private; the avatar engine needs it for its
 * soft-alpha render types (cosmetic translucency without vanilla's 0.1 alpha cutout, see {@code Tex}). Empty on
 * older versions (they keep vanilla's entity render types).
 */
@Mixin(RenderType.class)
public interface RenderTypeInvoker {
    //? if >=1.21.11 {
    @Invoker("create")
    static RenderType vibey$create(String name, RenderSetup setup) {
        throw new AssertionError();
    }
    //?}
}
