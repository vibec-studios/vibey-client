package dev.henny.vibeyclient.module.info.tool;

import dev.henny.vibeyclient.module.info.InfoModule;
import dev.henny.vibeyclient.module.info.core.Durability;
import dev.henny.vibeyclient.module.info.core.WarnLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public final class ToolState {
    private static ItemStack stack = ItemStack.EMPTY;
    private static WarnLevel level = WarnLevel.NONE;
    private static int remaining;

    private ToolState() {}

    public static void update(InfoModule m, Minecraft mc) {
        clear();
        if (!m.toolActive()) return;
        if (!check(mc.player.getMainHandItem(), m)) check(mc.player.getOffhandItem(), m);
    }

    private static boolean check(ItemStack st, InfoModule m) {
        if (st.isEmpty() || !st.isDamageableItem()) return false;
        WarnLevel l = Durability.level(st.getMaxDamage(), st.getDamageValue(), m.toolWarnAt.get());
        if (l == WarnLevel.NONE) return false;
        stack = st;
        level = l;
        remaining = Durability.remaining(st.getMaxDamage(), st.getDamageValue());
        return true;
    }

    public static void clear() {
        stack = ItemStack.EMPTY;
        level = WarnLevel.NONE;
        remaining = 0;
    }

    public static ItemStack stack() {
        return stack;
    }

    public static WarnLevel level() {
        return level;
    }

    public static int remaining() {
        return remaining;
    }
}
