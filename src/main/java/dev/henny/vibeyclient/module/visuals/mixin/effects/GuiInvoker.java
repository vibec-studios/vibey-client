package dev.henny.vibeyclient.module.visuals.mixin.effects;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Gui.class)
public interface GuiInvoker {
    @Invoker("renderTextureOverlay")
    void vibey$textureOverlay(GuiGraphics graphics, Identifier texture, float alpha);
}
