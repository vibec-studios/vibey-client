package dev.henny.vibeyclient.gametest.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface WindowTitleInvoker {
    @Invoker("createTitle")
    String vibeyclient$createTitle();
}
