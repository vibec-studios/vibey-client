package dev.henny.vibeyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.entity.EquipmentSlot;

final class HudWorld {
    private HudWorld() {}

    static void prepare(HudTestKit t, TestSingleplayerContext world) {
        for (String c : new String[] {"time set 1500", "weather clear", "gamerule doDaylightCycle false",
                "gamerule announceAdvancements false", "gamerule fallDamage false", "gamerule keepInventory true",
                "item replace entity @a armor.head with minecraft:diamond_helmet",
                "item replace entity @a armor.chest with minecraft:netherite_chestplate",
                "item replace entity @a armor.legs with minecraft:iron_leggings",
                "item replace entity @a armor.feet with minecraft:diamond_boots",
                "give @a minecraft:arrow 48", "give @a minecraft:totem_of_undying 2",
                "effect give @a minecraft:speed 600 1 true", "effect give @a minecraft:night_vision 600 0 true"}) {
            world.getServer().runCommand(c);
        }
        world.getClientWorld().waitForChunksRender();
        int[] at = t.context.computeOnClient(mc -> new int[] {
                (int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ())});
        int top = at[1] + 24;
        world.getServer().runCommand(String.format("fill %d %d %d %d %d %d minecraft:glass", at[0] - 1, top, at[2] - 1, at[0] + 1, top, at[2] + 1));
        world.getServer().runCommand(String.format("tp @a %d.5 %d %d.5", at[0], top + 1, at[2]));
        t.context.waitTicks(20);
        world.getClientWorld().waitForChunksRender();
        t.context.runOnClient(mc -> {
            mc.options.hideGui = false;
            mc.player.setYRot(135f);
            mc.player.setXRot(-4f);
            for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
                var st = mc.player.getItemBySlot(slot);
                st.setDamageValue(st.getMaxDamage() / (slot == EquipmentSlot.CHEST ? 4 : 2));
            }
        });
        t.context.waitTicks(30);
    }
}
