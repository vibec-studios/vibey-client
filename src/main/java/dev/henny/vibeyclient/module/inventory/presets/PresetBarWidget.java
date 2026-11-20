package dev.henny.vibeyclient.module.inventory.presets;

import dev.henny.vibeyclient.module.inventory.InventoryModule;
import dev.henny.vibeyclient.module.inventory.core.HotbarPreset;
import dev.henny.vibeyclient.module.inventory.core.PresetSet;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.HudWidget;
import net.minecraft.world.item.ItemStack;

public final class PresetBarWidget extends HudWidget {
    private static final int PAD = 2, ICON = 8, ROW = ICON + 3, NUM_W = 8;

    private ItemStack[][] icons = new ItemStack[0][];
    private String[] numbers = new String[0];
    private int revision = -1;
    private int active;
    private int pending = -1;
    private boolean visible;

    @Override
    protected int rate() {
        return 20;
    }

    @Override
    protected void update(HudPaint p) {
        PresetSet set = Presets.set();
        if (revision != Presets.revision() || icons.length != set.size()) rebuild(set);
        active = set.active();
        pending = PresetKeys.pending();
        InventoryModule m = InventoryModule.get();
        visible = p.preview || (m != null && PresetHud.wanted(m));
        width = PAD * 2 + NUM_W + HotbarPreset.SIZE * (ICON + 1);
        height = PAD * 2 + icons.length * ROW - 3;
    }

    private void rebuild(PresetSet set) {
        revision = Presets.revision();
        icons = new ItemStack[set.size()][HotbarPreset.SIZE];
        numbers = new String[set.size()];
        for (int i = 0; i < set.size(); i++) {
            numbers[i] = Integer.toString(i + 1);
            for (int s = 0; s < HotbarPreset.SIZE; s++) icons[i][s] = ItemRefs.icon(set.get(i).get(s));
        }
    }

    @Override
    protected boolean shown(HudPaint p) {
        return visible;
    }

    @Override
    protected void paint(HudPaint p) {
        p.background();
        for (int i = 0; i < icons.length; i++) {
            int y = PAD + i * ROW;
            boolean on = i == active, chosen = i == pending;
            if (chosen) p.fillShape(PAD - 1, y - 1, p.w - PAD * 2 + 2, ICON + 2, Theme.alpha(Theme.BRAND, 0.35f));
            if (on) p.outlineShape(PAD - 1, y - 1, p.w - PAD * 2 + 2, ICON + 2, Theme.BRAND);
            if (on || chosen) p.accentText(numbers[i], PAD + 1, y);
            else p.dimText(numbers[i], PAD + 1, y, 0.7f);
            int x = PAD + NUM_W;
            for (ItemStack st : icons[i]) {
                if (!st.isEmpty()) {
                    p.g.push();
                    p.g.translate(x, y);
                    p.g.scale(0.5f, 0.5f);
                    p.g.g.renderItem(st, 0, 0);
                    p.g.pop();
                }
                x += ICON + 1;
            }
        }
    }
}
