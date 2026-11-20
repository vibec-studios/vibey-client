package dev.henny.vibeyclient.module.info.food;

import dev.henny.vibeyclient.module.info.InfoModule;
import dev.henny.vibeyclient.module.info.core.FoodGain;
import dev.henny.vibeyclient.module.info.core.FoodMath;
import dev.henny.vibeyclient.module.info.core.FoodTable;
import dev.henny.vibeyclient.module.info.core.FoodValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;

public final class FoodState {
    private static FoodGain gain;
    private static int food;
    private static float saturation;
    private static boolean risky;

    private FoodState() {}

    public static void update(InfoModule m, Minecraft mc) {
        FoodData data = mc.player.getFoodData();
        food = data.getFoodLevel();
        saturation = data.getSaturationLevel();
        gain = null;
        risky = false;
        if (!m.foodActive()) return;
        ItemStack stack = FoodItem.pick(mc);
        FoodValue v = FoodItem.value(stack);
        if (v == null) return;
        gain = FoodMath.eat(food, saturation, v.nutrition(), v.saturation());
        risky = FoodTable.risky(FoodItem.id(stack));
    }

    public static void clear() {
        gain = null;
        risky = false;
    }

    public static FoodGain gain() {
        return gain;
    }

    public static int food() {
        return food;
    }

    public static float saturation() {
        return saturation;
    }

    public static boolean risky() {
        return risky;
    }
}
