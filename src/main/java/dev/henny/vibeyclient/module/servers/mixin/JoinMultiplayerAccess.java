package dev.henny.vibeyclient.module.servers.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(JoinMultiplayerScreen.class)
public interface JoinMultiplayerAccess {
    @Accessor("serverSelectionList")
    ServerSelectionList vibey$list();

    @Accessor("selectButton")
    Button vibey$select();

    @Accessor("editButton")
    Button vibey$edit();

    @Accessor("deleteButton")
    Button vibey$delete();

    @Invoker("onSelectedChange")
    void vibey$selectedChanged();
}
