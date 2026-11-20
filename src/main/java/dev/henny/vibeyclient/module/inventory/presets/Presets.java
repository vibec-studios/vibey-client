package dev.henny.vibeyclient.module.inventory.presets;

import com.google.gson.JsonElement;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.module.inventory.InventoryModule;
import dev.henny.vibeyclient.module.inventory.click.ClickSender;
import dev.henny.vibeyclient.module.inventory.core.HotbarPreset;
import dev.henny.vibeyclient.module.inventory.core.PresetJson;
import dev.henny.vibeyclient.module.inventory.core.PresetSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public final class Presets {
    private static final String KEY = "presets";
    private static final PresetSet SET = new PresetSet(3);
    private static InventoryModule module;
    private static int revision;
    private static long switchedAt;

    private Presets() {}

    public static void load(InventoryModule m) {
        module = m;
        SET.resize(m.presetCount.get());
        JsonElement e = m.settings().data().get(KEY);
        if (e != null && e.isJsonObject()) PresetJson.read(e.getAsJsonObject(), SET);
        revision++;
    }

    public static PresetSet set() {
        return SET;
    }

    public static int revision() {
        return revision;
    }

    public static long switchedAt() {
        return switchedAt;
    }

    public static void resize(int count) {
        SET.resize(count);
        store();
    }

    public static void saveActive() {
        var player = Minecraft.getInstance().player;
        if (module == null || player == null || !module.presetsActive()) return;
        SET.put(SET.active(), ItemRefs.hotbar(player.getInventory()));
        store();
        switchedAt = System.currentTimeMillis();
        Notify.success(module.tr("preset.saved", SET.active() + 1), "");
    }

    public static boolean apply(int index) {
        if (module == null || !module.presetsActive()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.screen == null || mc.screen instanceof InventoryScreen)) return false;
        SET.select(index);
        store();
        switchedAt = System.currentTimeMillis();
        HotbarPreset preset = SET.get(SET.active());
        return preset.blank() || ClickSender.offer(new PresetIntent(preset));
    }

    private static void store() {
        revision++;
        if (module == null) return;
        module.settings().data().add(KEY, PresetJson.write(SET));
        module.settings().markDirty();
    }
}
