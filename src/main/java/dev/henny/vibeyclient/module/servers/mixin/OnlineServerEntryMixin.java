package dev.henny.vibeyclient.module.servers.mixin;

import dev.henny.vibeyclient.module.servers.screen.ServerHover;
import dev.henny.vibeyclient.module.servers.screen.ServerSwap;
import dev.henny.vibeyclient.module.servers.screen.StarMark;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
abstract class OnlineServerEntryMixin {
    @Shadow @Final private ServerData serverData;

    @Inject(method = "renderContent", at = @At("TAIL"))
    private void vibey$star(GuiGraphics g, int mouseX, int mouseY, boolean hovered, float delta, CallbackInfo ci) {
        ServerSelectionList.Entry self = (ServerSelectionList.Entry) (Object) this;
        if (hovered) ServerHover.mark(self);
        StarMark.draw(g, self, serverData, mouseX, mouseY, hovered);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void vibey$starClick(MouseButtonEvent e, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (StarMark.click((ServerSelectionList.Entry) (Object) this, serverData, e.x(), e.y())) cir.setReturnValue(true);
    }

    @Inject(method = "swap", at = @At("HEAD"), cancellable = true)
    private void vibey$swap(int i, int j, CallbackInfo ci) {
        if (ServerSwap.swap(i, j)) ci.cancel();
    }
}
