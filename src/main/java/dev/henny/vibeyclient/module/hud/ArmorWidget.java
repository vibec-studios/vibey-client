package dev.henny.vibeyclient.module.hud;

import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.HudWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Worn armour (and optionally the held item) with durability as percent, bar or value; vertical or in a row. */
final class ArmorWidget extends HudWidget {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
    private static ItemStack[] sample;

    private final ItemStack[] stacks = new ItemStack[5];
    private final String[] texts = new String[5];
    private final float[] frac = new float[5];
    private final int[] textW = new int[5];
    private int count;
    private boolean horizontal, bar, text;
    private int maxTextW;

    @Override
    protected int rate() {
        return 4;
    }

    private static ItemStack[] sample() {
        if (sample == null) {
            ItemStack h = new ItemStack(Items.DIAMOND_HELMET), c = new ItemStack(Items.NETHERITE_CHESTPLATE),
                    l = new ItemStack(Items.DIAMOND_LEGGINGS), b = new ItemStack(Items.IRON_BOOTS), s = new ItemStack(Items.DIAMOND_SWORD);
            h.setDamageValue(h.getMaxDamage() / 8);
            c.setDamageValue(c.getMaxDamage() / 3);
            l.setDamageValue(l.getMaxDamage() / 2);
            b.setDamageValue(b.getMaxDamage() * 4 / 5);
            s.setDamageValue(s.getMaxDamage() / 4);
            sample = new ItemStack[] {h, c, l, b, s};
        }
        return sample;
    }

    @Override
    protected void update(HudPaint p) {
        Player pl = Minecraft.getInstance().player;
        String mode = p.option("mode");
        horizontal = "row".equals(p.option("layout"));
        bar = mode.equals("bar") || mode.equals("both");
        text = !mode.equals("bar");
        boolean hand = p.flag("hand");
        count = 0;
        boolean useSample = pl == null || (p.preview && isEmpty(pl));
        maxTextW = 0;
        for (int i = 0; i < SLOTS.length; i++) {
            if (i == 4 && !hand) continue;
            ItemStack st = useSample ? sample()[i] : pl.getItemBySlot(SLOTS[i]);
            if (st == null || st.isEmpty()) continue;
            stacks[count] = st;
            float f = st.isDamageableItem() && st.getMaxDamage() > 0 ? 1f - st.getDamageValue() / (float) st.getMaxDamage() : -1f;
            frac[count] = f;
            String t;
            if (f < 0) t = st.getCount() > 1 ? Integer.toString(st.getCount()) : "";
            else if (mode.equals("value")) t = (st.getMaxDamage() - st.getDamageValue()) + "";
            else t = Math.round(f * 100) + "%";
            texts[count] = text ? t : "";
            textW[count] = p.g.width(texts[count]);
            maxTextW = Math.max(maxTextW, textW[count]);
            count++;
        }
        if (horizontal) {
            width = Math.max(1, count) * 22 + 2;
            height = 16 + 4 + (text ? 10 : 0) + (bar ? 3 : 0);
        } else {
            width = 16 + 6 + (text ? maxTextW + 3 : 0) + (bar && !text ? 0 : 0);
            height = Math.max(1, count) * 18 + 2;
        }
    }

    private static boolean isEmpty(Player pl) {
        for (int i = 0; i < 4; i++) if (!pl.getItemBySlot(SLOTS[i]).isEmpty()) return false;
        return true;
    }

    @Override
    protected boolean shown(HudPaint p) {
        return count > 0;
    }

    @Override
    protected void paint(HudPaint p) {
        if (count == 0) return;
        p.background();
        int a = p.align();
        for (int i = 0; i < count; i++) {
            int x, y;
            if (horizontal) {
                x = (p.w - width) / 2 + 3 + i * 22;
                y = 2;
                p.g.g.renderItem(stacks[i], x, y);
                if (bar && frac[i] >= 0) durability(p, x + 1, y + 17, 14, frac[i]);
                if (text) p.text(texts[i], x + 8 - textW[i] / 2, y + 17 + (bar ? 3 : 0) + 1);
            } else {
                y = (p.h - height) / 2 + 1 + i * 18;
                boolean right = a > 0;
                int ix = right ? p.w - 3 - 16 : 3;
                p.g.g.renderItem(stacks[i], ix, y);
                if (bar && frac[i] >= 0) durability(p, ix + 1, y + 15, 14, frac[i]);
                if (text) {
                    int tx = right ? ix - 3 - textW[i] : ix + 19;
                    int col = frac[i] >= 0 && frac[i] < 0.15f ? Theme.DANGER : 0;
                    if (col != 0) p.plain(texts[i], tx, y + 4, col);
                    else p.text(texts[i], tx, y + 4);
                }
            }
        }
    }

    private static void durability(HudPaint p, int x, int y, int w, float f) {
        int c = f > 0.5f ? Theme.mix(0xFFFFD166, 0xFF7BE06B, (f - 0.5f) * 2) : Theme.mix(0xFFFF5470, 0xFFFFD166, f * 2);
        p.g.rect(x, y, w, 2, 0xC0000000);
        p.g.rect(x, y, Math.max(1, Math.round(w * f)), 1, c);
    }
}
