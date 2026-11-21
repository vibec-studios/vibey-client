package dev.henny.vibeyclient.module.info.card;

import dev.henny.vibeyclient.module.info.InfoModule;
import dev.henny.vibeyclient.module.info.core.HealthRule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
//? if >=1.21.11 {
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
//?} else
/*import net.minecraft.world.entity.boss.EnderDragonPart;*/
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

public final class EntityCards {
    private EntityCards() {}

    public static CardData read(Entity hit, Player player, InfoModule m) {
        Entity e = hit instanceof EnderDragonPart part ? part.parentMob : hit;
        String title = e.getDisplayName().getString();
        String mod = m.cardMod.on() ? ModNames.of(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace()) : null;
        return new CardData(null, e, icon(e), title, mod, null, CardData.NO_GROWTH, health(e, player));
    }

    private static String health(Entity e, Player player) {
        if (!(e instanceof LivingEntity living)) return null;
        boolean vehicle = e == player.getVehicle();
        boolean boss = e instanceof WitherBoss || e instanceof EnderDragon;
        return HealthRule.visible(vehicle, boss) ? HealthRule.label(living.getHealth(), living.getMaxHealth()) : null;
    }

    private static ItemStack icon(Entity e) {
        ItemStack pick = e.getPickResult();
        if (pick != null && !pick.isEmpty()) return pick;
        //? if >=26.1 {
        /*return SpawnEggItem.byId(e.getType()).map(h -> new ItemStack(h.value())).orElse(ItemStack.EMPTY);
        *///?} else {
        SpawnEggItem egg = SpawnEggItem.byId(e.getType());
        return egg != null ? new ItemStack(egg) : ItemStack.EMPTY;
        //?}
    }
}
