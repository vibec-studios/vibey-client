package dev.henny.vibeyclient.module.info.light;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public final class LightSources {
    private LightSources() {}

    public static boolean holding(Player p) {
        return emits(p.getMainHandItem()) || emits(p.getOffhandItem());
    }

    private static boolean emits(ItemStack st) {
        return st.getItem() instanceof BlockItem b && b.getBlock().defaultBlockState().getLightEmission() > 0;
    }
}
