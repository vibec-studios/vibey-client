package dev.henny.vibeyclient.module.info.card;

import dev.henny.vibeyclient.module.info.core.HarvestInfo;
import dev.henny.vibeyclient.module.info.core.ToolKind;
import dev.henny.vibeyclient.module.info.core.ToolTier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public final class ToolIcons {
    private static final Map<String, ItemStack> CACHE = new HashMap<>();

    private ToolIcons() {}

    public static ItemStack of(HarvestInfo h) {
        if (h == null || !h.showTool()) return ItemStack.EMPTY;
        return CACHE.computeIfAbsent(h.kind() + ":" + h.tier(), k -> new ItemStack(item(h.kind(), h.tier())));
    }

    private static Item item(ToolKind kind, ToolTier tier) {
        return switch (kind) {
            case PICKAXE -> pick(tier, Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE);
            case AXE -> pick(tier, Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE);
            case SHOVEL -> pick(tier, Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.DIAMOND_SHOVEL);
            case HOE -> pick(tier, Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.DIAMOND_HOE);
            case SWORD -> pick(tier, Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD);
            case SHEARS -> Items.SHEARS;
            case NONE -> Items.AIR;
        };
    }

    private static Item pick(ToolTier tier, Item wood, Item stone, Item iron, Item diamond) {
        return switch (tier) {
            case STONE -> stone;
            case IRON -> iron;
            case DIAMOND -> diamond;
            default -> wood;
        };
    }
}
