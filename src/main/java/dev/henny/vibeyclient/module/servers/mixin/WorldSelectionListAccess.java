package dev.henny.vibeyclient.module.servers.mixin;

import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(WorldSelectionList.class)
public interface WorldSelectionListAccess {
    @Accessor("filter")
    String vibey$filter();

    @Accessor("currentlyDisplayedLevels")
    List<LevelSummary> vibey$levels();

    @Invoker("fillLevels")
    void vibey$fill(String filter, List<LevelSummary> levels);
}
