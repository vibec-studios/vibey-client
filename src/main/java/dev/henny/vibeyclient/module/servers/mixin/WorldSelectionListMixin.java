package dev.henny.vibeyclient.module.servers.mixin;

import dev.henny.vibeyclient.module.servers.worlds.WorldListView;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(WorldSelectionList.class)
abstract class WorldSelectionListMixin {
    @Shadow @Final WorldSelectionList.EntryType entryType;

    @ModifyVariable(method = "fillLevels", at = @At("HEAD"), argsOnly = true)
    private List<LevelSummary> vibey$order(List<LevelSummary> levels) {
        return entryType == WorldSelectionList.EntryType.SINGLEPLAYER ? WorldListView.arrange(levels) : levels;
    }
}
