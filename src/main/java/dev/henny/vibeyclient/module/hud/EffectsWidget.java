package dev.henny.vibeyclient.module.hud;

import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.HudWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

final class EffectsWidget extends HudWidget {
    private static final int MAX = 12, ROW = 20;

    private final Identifier[] icons = new Identifier[MAX];
    private final String[] names = new String[MAX];
    private final String[] times = new String[MAX];
    private final boolean[] ending = new boolean[MAX];
    private final boolean[] bad = new boolean[MAX];
    private final int[] textW = new int[MAX];
    private int count;
    private boolean detailed;
    private final List<MobEffectInstance> sorted = new ArrayList<>();

    @Override
    protected int rate() {
        return 4;
    }

    @Override
    protected void update(HudPaint p) {
        Player pl = Minecraft.getInstance().player;
        detailed = p.detailed();
        count = 0;
        sorted.clear();
        if (pl != null) sorted.addAll(pl.getActiveEffects());
        if (sorted.isEmpty() && p.preview) {
            sorted.add(new MobEffectInstance(MobEffects.SPEED, 20 * 95, 1));
            sorted.add(new MobEffectInstance(MobEffects.STRENGTH, 20 * 8, 0));
            sorted.add(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 180, 0));
        }
        sorted.sort((a, b) -> Integer.compare(a.isInfiniteDuration() ? Integer.MAX_VALUE : a.getDuration(),
                b.isInfiniteDuration() ? Integer.MAX_VALUE : b.getDuration()));
        int maxW = 0;
        for (MobEffectInstance e : sorted) {
            if (count >= MAX) break;
            Holder<MobEffect> h = e.getEffect();
            icons[count] = Gui.getMobEffectSprite(h);
            int d = e.getDuration();
            times[count] = e.isInfiniteDuration() ? "∞" : (d / 20 / 60) + ":" + String.format(java.util.Locale.ROOT, "%02d", (d / 20) % 60);
            ending[count] = !e.isInfiniteDuration() && d < 200;
            bad[count] = !h.value().isBeneficial();
            String name = h.value().getDisplayName().getString();
            if (e.getAmplifier() > 0) name += " " + Component.translatable("enchantment.level." + (e.getAmplifier() + 1)).getString();
            names[count] = name;
            int w = detailed ? Math.max(p.g.width(name), p.g.width(times[count])) : p.g.width(times[count]);
            textW[count] = w;
            maxW = Math.max(maxW, w);
            count++;
        }
        width = 4 + 18 + 4 + maxW + 4;
        height = Math.max(1, count) * ROW + 2;
    }

    @Override
    protected boolean shown(HudPaint p) {
        return count > 0;
    }

    @Override
    protected void paint(HudPaint p) {
        if (count == 0) return;
        p.background();
        boolean right = p.align() > 0;
        for (int i = 0; i < count; i++) {
            int y = 2 + i * ROW;
            int ix = right ? p.w - 4 - 18 : 4;
            float blink = ending[i] ? 0.45f + 0.55f * (float) Math.abs(Math.sin(p.now / 220.0)) : 1f;
            if (blink < 1f) {
                p.g.g.blitSprite(RenderPipelines.GUI_TEXTURED, icons[i], ix, y, 18, 18, Theme.alpha(0xFFFFFFFF, blink));
            } else {
                p.g.g.blitSprite(RenderPipelines.GUI_TEXTURED, icons[i], ix, y, 18, 18);
            }
            int tx = right ? ix - 4 - textW[i] : ix + 22;
            if (detailed) {
                int nw = p.g.width(names[i]);
                int nx = right ? ix - 4 - nw : tx;
                if (bad[i]) p.plain(names[i], nx, y + 1, Theme.DANGER);
                else p.text(names[i], nx, y + 1);
                int tw = p.g.width(times[i]);
                p.dimText(times[i], right ? ix - 4 - tw : tx, y + 10, 0.7f * blink);
            } else {
                int tw = p.g.width(times[i]);
                p.dimText(times[i], right ? ix - 4 - tw : tx, y + 5, blink);
            }
        }
    }
}
