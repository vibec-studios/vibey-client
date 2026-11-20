package dev.henny.vibeyclient.ui.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.vibeyclient.api.setting.SettingsGroup;

import java.util.Map;

final class LegacyHudFile {
    private LegacyHudFile() {}

    static void migrate(JsonObject data, SettingsGroup target) {
        try {
            SettingsGroup old = new SettingsGroup("hud");
            old.load();
            for (Map.Entry<String, JsonElement> en : old.data().entrySet()) {
                if (en.getValue().isJsonObject() && en.getValue().getAsJsonObject().has("anchor")) {
                    data.add(en.getKey(), en.getValue().deepCopy());
                }
            }
            if (data.size() > 0) {
                data.addProperty("_saved", true);
                target.markDirty();
            }
        } catch (RuntimeException e) {
            dev.henny.vibeyclient.Vibey.LOG.warn("Could not migrate the old HUD layout", e);
        }
    }
}
