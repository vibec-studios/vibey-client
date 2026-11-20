package dev.henny.vibeyclient.module.servers.mixin;

import dev.henny.vibeyclient.module.servers.screen.ServerListView;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(ServerSelectionList.class)
abstract class ServerSelectionListMixin {
    @Inject(method = "updateOnlineServers", at = @At("HEAD"))
    private void vibey$restorePings(ServerList list, CallbackInfo ci) {
        ServerListView.restore(list);
    }

    @ModifyArg(method = "refreshEntries", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/multiplayer/ServerSelectionList;replaceEntries(Ljava/util/Collection;)V"))
    private Collection<ServerSelectionList.Entry> vibey$arrange(Collection<ServerSelectionList.Entry> entries) {
        return ServerListView.arrange(entries);
    }
}
