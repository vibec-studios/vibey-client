package dev.henny.vibeyclient.module.screens.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerAccess {
    @Accessor("leftPos")
    int vibey$leftPos();

    @Accessor("leftPos")
    void vibey$setLeftPos(int x);

    @Accessor("topPos")
    int vibey$topPos();

    @Accessor("topPos")
    void vibey$setTopPos(int y);

    @Accessor("imageWidth")
    int vibey$imageWidth();

    @Accessor("imageHeight")
    int vibey$imageHeight();
}
