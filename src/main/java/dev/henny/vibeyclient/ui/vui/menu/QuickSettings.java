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

public final class QuickSettings {
    public final ListState<SettingRow> quick = new ListState<>();
    public final ListState<SettingRow> quickMore = new ListState<>();
    public final State<Boolean> quickMoreOpen = new State<>(false);
    private final ActionSetting copy = new ActionSetting("copyInfo", BugInfo::copy);
    private String lang = "";

    public void sync() {
        if (!Lang.code().equals(lang)) {
            lang = Lang.code();
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
        main.add(Rows.of(GlobalSettings.animations).labelled(Lang.tr("ui.quick.motion"), ""));
        main.add(Rows.of(GlobalSettings.onlineFeatures).labelled(Lang.tr("ui.quick.online"), Lang.tr("ui.quick.onlineTip")));
        main.add(Rows.of(GlobalSettings.openMenu).labelled(Lang.tr("ui.quick.menuKey"), ""));
        quick.replaceAll(main);
        List<SettingRow> more = new ArrayList<>();
        more.add(Rows.of(GlobalSettings.backendUrl).labelled(Lang.tr("ui.quick.server"), Lang.tr("ui.quick.serverTip")));
        more.add(Rows.of(copy).labelled(Lang.tr("ui.quick.info"), Lang.tr("ui.quick.infoTip")).withButton(Lang.tr("ui.settings.copyInfo")));
        if (GlobalSettings.classicUi.isVisible()) more.add(Rows.of(GlobalSettings.classicUi));
        quickMore.replaceAll(more);
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
