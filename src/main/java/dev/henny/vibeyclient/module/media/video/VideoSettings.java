package dev.henny.vibeyclient.module.media.video;

import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.client.Keys;

import java.util.function.BooleanSupplier;

/**
 * The few options of the video screens, declared into the host module's settings group (labels:
 * {@code <host>.setting.<key>}, keybind name {@code key.vibeyclient.<host>.<key>}). Must be built while the host
 * is constructed (keybinds can't be created later).
 */
public final class VideoSettings {
    /** What happens with screens other players share. */
    public enum Others { ASK, ALWAYS, NEVER }

    public enum Quality {
        P360(360), P480(480), P720(720), P1080(1080);

        public final int lines;

        Quality(int lines) {
            this.lines = lines;
        }
    }

    public final SettingsGroup group;
    public final KeybindSetting key;
    public final EnumSetting<Others> others;
    public final IntSetting volume;
    public final ActionSetting editOverlay;
    public final EnumSetting<Quality> quality;
    public final IntSetting fps;
    public final IntSetting distance;
    public final ActionSetting removeAll;

    public VideoSettings(String hostId, SettingsGroup g, BooleanSupplier active) {
        this.group = g;
        key = g.add(new KeybindSetting(hostId, "screens", Keys.N)).onPress(() -> Video.get().openMenu());
        key.setActive(active);
        others = g.add(new EnumSetting<>("others", Others.ASK));
        volume = g.add(new IntSetting("volume", 80, 0, 100, 5)).unit("%");
        editOverlay = g.add(new ActionSetting("editOverlay", () -> Video.get().editOverlay()));
        quality = g.add(new EnumSetting<>("quality", Quality.P720)).advanced();
        fps = g.add(new IntSetting("fps", 30, 10, 60, 5)).advanced();
        distance = g.add(new IntSetting("distance", 48, 16, 128, 8)).unit(" m").advanced();
        removeAll = g.add(new ActionSetting("removeAll", () -> Video.get().removeAllOwn())).advanced();
    }
}
