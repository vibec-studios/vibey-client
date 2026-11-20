package dev.henny.vibeyclient.module.servers.mixin;

import dev.henny.vibeyclient.module.servers.drag.ListDrag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
abstract class SelectionListDragMixin {
    @Inject(method = "renderListItems", at = @At("HEAD"))
    private void vibey$dragShift(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ListDrag.shift(this);
    }

    @Inject(method = "renderListItems", at = @At("TAIL"))
    private void vibey$dragRestore(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ListDrag.restore(this, g);
    }
}
