package dev.henny.vibeyclient.module.hud.mixin;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if >=26.2 {
/*@Mixin(net.minecraft.client.gui.Hud.class)
*///?} else
@Mixin(net.minecraft.client.gui.Gui.class)
public interface GuiTitleAccessor {
    @Accessor("title")
    Component vibey$title();

    @Accessor("subtitle")
    Component vibey$subtitle();

    @Accessor("titleTime")
    int vibey$titleTime();
}
