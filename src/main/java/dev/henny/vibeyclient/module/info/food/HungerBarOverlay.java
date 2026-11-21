package dev.henny.vibeyclient.module.info.food;

import dev.henny.vibeyclient.module.info.InfoModule;
import dev.henny.vibeyclient.module.info.core.Colors;
import dev.henny.vibeyclient.module.info.core.FoodGain;
import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

public final class HungerBarOverlay {
    private static final Object FULL = Compat.id("minecraft", "hud/food_full");
    private static final Object HALF = Compat.id("minecraft", "hud/food_half");
    private static final Object FULL_HUNGER = Compat.id("minecraft", "hud/food_full_hunger");
    private static final Object HALF_HUNGER = Compat.id("minecraft", "hud/food_half_hunger");
    private static final int SATURATION = 0xFFFFC94D;
    private static final long PULSE_MS = 1200;

    private HungerBarOverlay() {}

    public static void draw(Gfx g, InfoModule m) {
        FoodGain gain = FoodState.gain();
        if (gain == null || !barVisible()) return;
        int right = g.screenWidth() / 2 + 91;
        int y = g.screenHeight() - 39;
        float a = pulse();
        drawGain(g, gain, right, y, a, FoodState.risky());
        if (m.saturationActive()) drawSaturation(g, gain, right, y, a);
    }

    private static boolean barVisible() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return false;
        return !(mc.player.getVehicle() instanceof LivingEntity v && v.showVehicleHealth());
    }

    private static float pulse() {
        double phase = (System.currentTimeMillis() % PULSE_MS) / (double) PULSE_MS;
        return (float) (0.45 + 0.45 * (0.5 + 0.5 * Math.cos(phase * Math.PI * 2)));
    }

    private static void drawGain(Gfx g, FoodGain gain, int right, int y, float a, boolean risky) {
        int before = FoodState.food();
        int color = Colors.alpha(0xFFFFFFFF, a);
        for (int i = 0; i < 10; i++) {
            int full = i * 2 + 2;
            if (gain.food() <= i * 2 || before >= full) continue;
            boolean whole = gain.food() >= full;
            Object sprite = risky ? (whole ? FULL_HUNGER : HALF_HUNGER) : (whole ? FULL : HALF);
            g.sprite(sprite, right - i * 8 - 9, y, 9, 9, color);
        }
    }

    private static void drawSaturation(Gfx g, FoodGain gain, int right, int y, float a) {
        float now = FoodState.saturation();
        for (int i = 0; i < 10; i++) {
            int x = right - i * 8 - 9;
            int have = Math.round(Math.max(0f, Math.min(2f, now - i * 2)) * 4);
            int after = Math.round(Math.max(0f, Math.min(2f, gain.saturation() - i * 2)) * 4);
            if (have > 0) g.rect(x + 9 - have, y + 9, have, 1, SATURATION);
            if (after > have) g.rect(x + 9 - after, y + 9, after - have, 1, Colors.alpha(SATURATION, a));
        }
    }
}
