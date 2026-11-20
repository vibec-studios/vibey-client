package dev.henny.vibeyclient.module.hud.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface GuiTitleAccessor {
    @Accessor("title")
    Component vibey$title();

    @Accessor("subtitle")
    Component vibey$subtitle();

    @Accessor("titleTime")
    int vibey$titleTime();
}
