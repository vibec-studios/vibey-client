package dev.henny.vibeyclient.module.inventory.tooltip;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.inventory.InventoryModule;
import dev.henny.vibeyclient.module.inventory.core.DurabilityText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class DurabilityLine {
    private DurabilityLine() {}

    static void add(InventoryModule m, ItemStack stack, List<Component> lines) {
        if (!m.durabilityActive() || stack.isEmpty() || !stack.isDamageableItem()) return;
        int pct = DurabilityText.percent(stack.getMaxDamage(), stack.getDamageValue());
        int left = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
        Component line = Component.literal(Lang.tr("inventory.tooltip.durability", pct, left))
                .withColor(DurabilityText.color(pct) & 0xFFFFFF);
        lines.add(Math.min(1, lines.size()), line);
    }
}
