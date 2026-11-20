package dev.henny.vibeyclient.module.inventory.search;

import dev.henny.vibeyclient.module.inventory.core.ItemFacts;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;

final class ItemFactsReader {
    private static final int MAX_EXTRAS = 64;

    private ItemFactsReader() {}

    static ItemFacts read(ItemStack stack) {
        List<String> extras = new ArrayList<>();
        enchantments(stack, extras);
        ItemContainerContents box = stack.get(DataComponents.CONTAINER);
        if (box != null) for (ItemStack inner : box.nonEmptyItems()) if (extras.size() < MAX_EXTRAS) extras.add(name(inner));
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) for (ItemStack inner : bundle.items()) if (extras.size() < MAX_EXTRAS) extras.add(name(inner));
        return ItemFacts.of(stack.getHoverName().getString(), BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), extras);
    }

    private static void enchantments(ItemStack stack, List<String> out) {
        ItemEnchantments list = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (Holder<Enchantment> e : list.keySet()) out.add(Enchantment.getFullname(e, list.getLevel(e)).getString());
    }

    private static String name(ItemStack stack) {
        return stack.getHoverName().getString();
    }
}
