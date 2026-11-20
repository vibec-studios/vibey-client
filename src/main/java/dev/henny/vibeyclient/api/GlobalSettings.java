package dev.henny.vibeyclient.api;

import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.api.setting.Settings.StringSetting;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.client.Keys;

/** Settings that apply to the whole client ({@code config/vibeyclient/core.json}), shown on the "General" tab. */
public final class GlobalSettings {
    private GlobalSettings() {}

    public enum Language { AUTO, GERMAN, ENGLISH }

    public static final SettingsGroup GROUP = new SettingsGroup("core");

    public static final EnumSetting<Language> language = GROUP.add(new EnumSetting<>("language", Language.AUTO)).section("look");

    /** Size of the Vibey screens: the game's GUI scale or a fixed factor (whole numbers stay crisp). */
    public enum UiScale {
        GAME(0), X2(2), X3(3), X4(4);

        public final int factor;

        UiScale(int factor) {
            this.factor = factor;
        }
    }

    public static final EnumSetting<UiScale> uiScale = GROUP.add(new EnumSetting<>("uiScale", UiScale.GAME)).section("look")
            .onChange(v -> dev.henny.vibeyclient.ui.UiScale.refresh());

    public static final BoolSetting animations = GROUP.add(new BoolSetting("animations", true)).section("look");

    /** Off: no backend connection at all, social hidden, cosmetics only local / via server packets. */
    public static final BoolSetting onlineFeatures = GROUP.add(new BoolSetting("onlineFeatures", true)).section("online")
            .onChange(on -> Events.post(new VibeyEvents.OnlineFeaturesChanged(on)));

    /**
     * {@code <instance>/.vibe/session.json} for the Vibe Launcher (Discord status: world, server, invite link). Local
     * only; off deletes the file. Contract: vibe-launcher docs/PLAY-LINKS.md §2.
     */
    public static final BoolSetting launcherStatus = GROUP.add(new BoolSetting("launcherStatus", true)).section("online");

    public static final KeybindSetting openMenu = GROUP.add(new KeybindSetting("core", "open_menu", Keys.RIGHT_SHIFT));

    /** Default = BuildInfo.DEFAULT_BACKEND: our backend, official builds only (forks: own default_backend, https://github.com/vibec-studios/.github/blob/main/SERVICES.md). */
    public static final StringSetting backendUrl = GROUP.add(new StringSetting("backendUrl", BuildInfo.DEFAULT_BACKEND, 256)).advanced();

    public static final BoolSetting classicUi = GROUP.add(new BoolSetting("classicUi", false)).advanced()
            .visibleWhen(() -> GlobalSettings.classicUi.on());

    public static final ActionSetting resetBackend = GROUP.add(new ActionSetting("resetBackend",
            () -> GlobalSettings.backendUrl.reset())).advanced();

    /** Backend base URL without a trailing slash. */
    public static String backend() {
        String s = backendUrl.get().trim();
        if (s.isEmpty()) s = BuildInfo.DEFAULT_BACKEND;
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    public static boolean online() {
        return onlineFeatures.on();
    }

    public static boolean animate() {
        return animations.on();
    }

    public static void load() {
        // 0.3.0: the old default backend (vibe.henny.dev) moves to vibe.axolite.app, own addresses stay
        GROUP.migration(dev.henny.vibeyclient.backend.DomainMove.ID, dev.henny.vibeyclient.backend.DomainMove::migrateCore);
        GROUP.load();
    }

    /** True once the first-start welcome was finished or skipped. */
    public static boolean onboarded() {
        com.google.gson.JsonElement e = GROUP.data().get("onboarded");
        return e != null && e.isJsonPrimitive() && e.getAsBoolean();
    }

    public static void setOnboarded(boolean done) {
        GROUP.data().addProperty("onboarded", done);
        GROUP.markDirty();
    }
}
