package dev.henny.vibeyclient.module.servers.mixin;

import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public interface OnlineServerEntryInvoker {
    @Invoker("refreshStatus")
    void vibey$refreshStatus();
}
