package dev.henny.vibeyclient.ui.vui.menu;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.ui.vui.settings.Option;
import dev.henny.vibeyclient.ui.vui.settings.Rows;
import dev.henny.vibeyclient.ui.vui.settings.SettingRow;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class QuickSettings {
    public final ListState<SettingRow> quick = new ListState<>();
    public final ListState<SettingRow> quickMore = new ListState<>();
    public final State<Boolean> quickMoreOpen = new State<>(false);
    public final ListState<QuickHit> quickHits = new ListState<>();
    public final State<String> quickFocus = new State<>("");
    private final ActionSetting copy = new ActionSetting("copyInfo", BugInfo::copy);
    private String lang = "";
    private String query = "";

    public void filter(String q) {
        query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<QuickHit> hits = new ArrayList<>();
        if (!query.isEmpty()) {
            for (SettingRow r : quick.get()) if (matches(r)) hits.add(new QuickHit(r.getId(), r.getLabel(), false));
            for (SettingRow r : quickMore.get()) if (matches(r)) hits.add(new QuickHit(r.getId(), r.getLabel(), true));
        }
        quickHits.replaceAll(hits);
    }

    public void focus(QuickHit h) {
        quickMoreOpen.set(h.isMore());
        quickFocus.set(h.getId());
    }

    private boolean matches(SettingRow r) {
        String name = Lang.tr(r.setting().nameKey());
        return (r.getLabel() + " " + name).toLowerCase(Locale.ROOT).contains(query);
    }

    public void sync() {
        String key = Lang.code() + GlobalSettings.ownAccent.on();
        if (!key.equals(lang)) {
            lang = key;
            build();
        }
        for (SettingRow r : quick.get()) r.sync();
        for (SettingRow r : quickMore.get()) r.sync();
    }

    private void build() {
        List<SettingRow> main = new ArrayList<>();
        main.add(Rows.of(GlobalSettings.language).labelled(Lang.tr("ui.quick.language"), "")
                .withOptions(languages(), true));
        main.add(Rows.of(GlobalSettings.uiScale).labelled(Lang.tr("ui.quick.size"), "").withOptions(sizes(), true));
        main.add(Rows.of(GlobalSettings.theme).labelled(Lang.tr("ui.quick.theme"), Lang.tr("ui.quick.themeTip")));
        main.add(Rows.of(GlobalSettings.animations).labelled(Lang.tr("ui.quick.motion"), ""));
        main.add(Rows.of(GlobalSettings.onlineFeatures).labelled(Lang.tr("ui.quick.online"), Lang.tr("ui.quick.onlineTip")));
        main.add(Rows.of(GlobalSettings.openMenu).labelled(Lang.tr("ui.quick.menuKey"), ""));
        quick.replaceAll(main);
        List<SettingRow> more = new ArrayList<>();
        more.add(Rows.of(GlobalSettings.ownAccent).labelled(Lang.tr("ui.quick.ownAccent"), Lang.tr("ui.quick.ownAccentTip")));
        if (GlobalSettings.ownAccent.on()) more.add(Rows.of(GlobalSettings.accent).labelled(Lang.tr("ui.quick.accent"), ""));
        more.add(Rows.of(GlobalSettings.backendUrl).labelled(Lang.tr("ui.quick.server"), Lang.tr("ui.quick.serverTip")));
        more.add(Rows.of(copy).labelled(Lang.tr("ui.quick.info"), Lang.tr("ui.quick.infoTip")).withButton(Lang.tr("ui.settings.copyInfo")));
        if (GlobalSettings.classicUi.isVisible()) more.add(Rows.of(GlobalSettings.classicUi));
        quickMore.replaceAll(more);
        filter(query);
    }

    static List<Option> languages() {
        return List.of(new Option("AUTO", Lang.tr("ui.quick.languageAuto")), new Option("ENGLISH", "English"),
                new Option("GERMAN", "Deutsch"));
    }

    private static List<Option> sizes() {
        return List.of(new Option("GAME", Lang.tr("ui.quick.sizeAuto")), new Option("X2", "2×"), new Option("X3", "3×"),
                new Option("X4", "4×"));
    }
}
