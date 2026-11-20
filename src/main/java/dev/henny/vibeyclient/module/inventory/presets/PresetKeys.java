package dev.henny.vibeyclient.module.inventory.presets;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.inventory.InventoryModule;
import dev.henny.vibeyclient.module.inventory.core.KeyGesture;
import net.minecraft.client.Minecraft;

public final class PresetKeys {
    static final long HOLD_MS = 250;

    private static final KeyGesture GESTURE = new KeyGesture(HOLD_MS);
    private static int pending = -1;

    private PresetKeys() {}

    public static void tick(InventoryModule m) {
        long now = System.nanoTime();
        boolean down = m.presetsActive() && Minecraft.getInstance().screen == null && m.presetKey.isDown();
        KeyGesture.Result r = GESTURE.update(down, now);
        if (GESTURE.holding(now) && pending < 0) pending = Presets.set().active();
        if (r == KeyGesture.Result.TAP) {
            if (ClientCompat.shiftDown()) Presets.saveActive();
            else Presets.apply(Presets.set().next());
        } else if (r == KeyGesture.Result.HOLD_END) {
            if (pending >= 0) Presets.apply(pending);
            pending = -1;
        }
    }

    public static boolean scroll(double dy) {
        InventoryModule m = InventoryModule.get();
        if (m == null || !m.presetsActive() || !GESTURE.down() || dy == 0) return false;
        if (pending < 0) pending = Presets.set().active();
        pending = Presets.set().step(pending, dy > 0 ? -1 : 1);
        GESTURE.markUsed();
        return true;
    }

    public static boolean holding() {
        return GESTURE.holding(System.nanoTime());
    }

    public static int pending() {
        return pending;
    }

    public static void reset() {
        GESTURE.reset();
        pending = -1;
    }
}
