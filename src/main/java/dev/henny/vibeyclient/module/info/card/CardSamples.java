package dev.henny.vibeyclient.module.info.card;

import dev.henny.vibeyclient.module.info.core.HarvestInfo;
import dev.henny.vibeyclient.module.info.core.HarvestVerdict;
import dev.henny.vibeyclient.module.info.core.ToolKind;
import dev.henny.vibeyclient.module.info.core.ToolTier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public final class CardSamples {
    private static CardData block;

    private CardSamples() {}

    public static CardData block() {
        if (block == null) {
            block = new CardData(null, null, new ItemStack(Items.DIAMOND_ORE), Blocks.DIAMOND_ORE.getName().getString(),
                    "Minecraft", new HarvestInfo(ToolKind.PICKAXE, ToolTier.IRON, HarvestVerdict.OK), CardData.NO_GROWTH,
                    null);
        }
        return block;
    }
}
