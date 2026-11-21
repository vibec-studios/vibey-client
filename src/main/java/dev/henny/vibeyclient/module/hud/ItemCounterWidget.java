package dev.henny.vibeyclient.module.hud;

import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.HudWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class ItemCounterWidget extends HudWidget {
    static final String[] PRESETS = {"minecraft:arrow", "minecraft:totem_of_undying", "minecraft:golden_apple",
            "minecraft:ender_pearl", "minecraft:firework_rocket", "minecraft:experience_bottle", "minecraft:cobweb",
            "minecraft:wind_charge", "minecraft:torch", "minecraft:cooked_beef"};

    private String itemId = "";
    private Item item = Items.ARROW;
    private ItemStack icon = new ItemStack(Items.ARROW);
    private String countText = "0";
    private String name = "";
    private int count;
    private boolean detailed;

    @Override
    protected int rate() {
        return 4;
    }

    static Item item(String id) {
        try {
            //? if >=1.21.11 {
            var rl = net.minecraft.resources.Identifier.tryParse(id);
            //?} else
            /*var rl = net.minecraft.resources.ResourceLocation.tryParse(id);*/
            if (rl == null) return Items.ARROW;
            Item it = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            return it == null || it == Items.AIR ? Items.ARROW : it;
        } catch (RuntimeException e) {
            return Items.ARROW;
        }
    }

    static String displayName(String id) {
        return new ItemStack(item(id)).getHoverName().getString();
    }

    static String held() {
        Player p = Minecraft.getInstance().player;
        if (p == null || p.getMainHandItem().isEmpty()) return null;
        return BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).toString();
    }

    @Override
    protected void update(HudPaint p) {
        String id = p.option("item");
        if (!id.equals(itemId)) {
            itemId = id;
            item = item(id);
            icon = new ItemStack(item);
            name = icon.getHoverName().getString();
        }
        detailed = p.detailed();
        Player pl = Minecraft.getInstance().player;
        int c = 0;
        if (pl != null) {
            Inventory inv = pl.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack st = inv.getItem(i);
                if (!st.isEmpty() && st.is(item)) c += st.getCount();
            }
        } else if (p.preview) {
            c = item == Items.TOTEM_OF_UNDYING ? 3 : 64;
        }
        count = c;
        countText = Integer.toString(c);
        width = 4 + 16 + 3 + (detailed ? p.g.width(name) + 4 : 0) + p.g.width(countText) + 4;
        height = 20;
    }

    @Override
    protected boolean shown(HudPaint p) {
        return count > 0 || p.flag("zero");
    }

    @Override
    protected void paint(HudPaint p) {
        p.background();
        boolean right = p.align() > 0;
        int cw = width - 8;
        int x = right ? p.w - 4 - cw : p.align() == 0 ? (p.w - cw) / 2 : 4;
        int y = (p.h - 16) / 2;
        p.g.item(icon, x, y);
        int tx = x + 19;
        if (detailed) {
            p.accentText(name, tx, y + 4);
            tx += p.g.width(name) + 4;
        }
        if (count == 0) p.dimText(countText, tx, y + 4, 0.45f);
        else p.text(countText, tx, y + 4);
    }
}
