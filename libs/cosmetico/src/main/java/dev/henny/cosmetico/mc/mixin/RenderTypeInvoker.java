package dev.henny.cosmetico.mc.mixin;

import org.spongepowered.asm.mixin.Mixin;
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.gen.Invoker;
//?} else
/*import net.minecraft.client.renderer.RenderType;*/

@Mixin(RenderType.class)
public interface RenderTypeInvoker {
    //? if >=1.21.11 {
    @Invoker("create")
    static RenderType cosmetico$create(String name, RenderSetup setup) {
        throw new AssertionError();
    }
    //?}
}
