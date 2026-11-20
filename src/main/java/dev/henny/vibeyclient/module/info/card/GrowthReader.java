package dev.henny.vibeyclient.module.info.card;

import dev.henny.vibeyclient.module.info.core.Growth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

public final class GrowthReader {
    private GrowthReader() {}

    public static int percent(BlockState s) {
        Block b = s.getBlock();
        if (b instanceof CropBlock crop) return Growth.percent(crop.getAge(s), crop.getMaxAge());
        if (!visibleAge(b)) return CardData.NO_GROWTH;
        for (Property<?> p : s.getProperties()) {
            if (p instanceof IntegerProperty ip && "age".equals(p.getName())) {
                int max = 0;
                for (Integer v : ip.getPossibleValues()) max = Math.max(max, v);
                return Growth.percent(s.getValue(ip), max);
            }
        }
        return CardData.NO_GROWTH;
    }

    private static boolean visibleAge(Block b) {
        return b instanceof StemBlock || b instanceof NetherWartBlock || b instanceof CocoaBlock
                || b instanceof SweetBerryBushBlock || b instanceof PitcherCropBlock;
    }
}
