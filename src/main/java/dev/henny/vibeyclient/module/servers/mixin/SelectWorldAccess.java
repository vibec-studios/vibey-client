package dev.henny.vibeyclient.module.servers.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SelectWorldScreen.class)
public interface SelectWorldAccess {
    @Accessor("list")
    WorldSelectionList vibey$list();

    @Accessor("searchBox")
    EditBox vibey$search();
}
