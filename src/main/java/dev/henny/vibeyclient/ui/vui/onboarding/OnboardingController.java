package dev.henny.vibeyclient.ui.vui.onboarding;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Setting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.ui.vui.WatchedState;
import dev.henny.vibeyclient.ui.vui.settings.Option;
import dev.henny.vui.bind.State;

import java.util.ArrayList;
import java.util.List;

public final class OnboardingController {
    public static final int STEPS = 3;

    public final State<Integer> step = new State<>(0);
    public final WatchedState<String> language = new WatchedState<>(GlobalSettings.language.get().name());
    public final WatchedState<Boolean> online = new WatchedState<>(GlobalSettings.online());
    private final List<KeyItem> keys = new ArrayList<>();
    private final Runnable done;

    public OnboardingController(Runnable done) {
        this.done = done;
        language.onChange(v -> GlobalSettings.language.set(GlobalSettings.Language.valueOf(v)));
        online.onChange(GlobalSettings.onlineFeatures::set);
        keys.add(new KeyItem("menu", "seal", "ui.onboarding.menu", GlobalSettings.openMenu));
        addKey("emotes", "wave", "ui.onboarding.emotes");
        addKey("hud", "hud", "ui.onboarding.hud");
    }

    private void addKey(String moduleId, String icon, String labelKey) {
        VibeyModule m = ModuleManager.get().get(moduleId);
        if (m == null || !m.status().loadable()) return;
        for (Setting<?, ?> s : m.settings().all()) {
            if (s instanceof KeybindSetting k) {
                keys.add(new KeyItem(moduleId, icon, labelKey, k));
                return;
            }
        }
    }

    public List<Feature> getFeatures() {
        return List.of(feature("friends", "social"), feature("cosmetics", "cosmetics"),
                feature("emotes", "emotes"), feature("hud", "hud"));
    }

    private static Feature feature(String id, String module) {
        VibeyModule m = ModuleManager.get().get(module);
        String icon = "module_" + module;
        String tip = m == null ? "" : dev.henny.vibeyclient.ui.vui.menu.ModuleLook.summary(m);
        return new Feature(id, icon, Lang.tr("ui.onboarding.feature." + id), tip);
    }

    public List<java.util.Map<String, Object>> getLanguages() {
        return Option.maps(List.of(new Option("AUTO", Lang.tr("ui.quick.languageAuto")), new Option("ENGLISH", "English"),
                new Option("GERMAN", "Deutsch")));
    }

    public List<KeyItem> getKeys() {
        return keys;
    }

    public void next() {
        if (step.peek() >= STEPS - 1) finish();
        else step.set(step.peek() + 1);
    }

    public void previous() {
        if (step.peek() > 0) step.set(step.peek() - 1);
    }

    public void go(int i) {
        if (i >= 0 && i < STEPS) step.set(i);
    }

    public void skip() {
        finish();
    }

    public void finish() {
        GlobalSettings.setOnboarded(true);
        done.run();
    }

    void sync() {
        language.quiet(GlobalSettings.language.get().name());
        online.quiet(GlobalSettings.online());
        for (KeyItem k : keys) k.sync();
    }
}
