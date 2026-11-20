package dev.henny.vibeyclient.module.inventory.tooltip;

import dev.henny.vibeyclient.module.inventory.InventoryModule;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;

public final class TooltipHooks {
    private static boolean registered;

    private TooltipHooks() {}

    public static void register(InventoryModule m) {
        if (registered) return;
        registered = true;
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> DurabilityLine.add(m, stack, lines));
        TooltipComponentCallback.EVENT.register(data -> data instanceof ContainerPreview p ? new ContainerPreviewTooltip(p) : null);
    }
}
