package dev.henny.vibeyclient.module.nametags;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.setting.OptionSetting;
import dev.henny.vibeyclient.client.ranks.MyRanksApi;
import dev.henny.vibeyclient.client.ranks.Ranks;
import dev.henny.vibeyclient.ranks.NameStyleDef;
import dev.henny.vibeyclient.ranks.OwnRanks;
import dev.henny.vibeyclient.ranks.RankDef;
import dev.henny.vibeyclient.ui.vui.ClassicUi;

import java.util.ArrayList;
import java.util.List;

final class MyNameSettings {
    private static final String NONE = "none";
    private static final String AUTO = "auto";

    private MyNameSettings() {}

    static void declare(NametagsModule m) {
        m.settings().add(new OptionSetting("myStyle", MyNameSettings::style, MyNameSettings::setStyle, () -> styles(m)))
                .section("mine").visibleWhen(() -> shown() && styles(m).size() > 1);
        m.settings().add(new OptionSetting("myRank", MyNameSettings::rank, MyNameSettings::setRank, () -> ranks(m)))
                .section("mine").visibleWhen(() -> shown() && ranks(m).size() > 2);
        m.settings().add(new OptionSetting("showRank", () -> String.valueOf(Ranks.own().showRank()),
                v -> MyRanksApi.setShowRank(Boolean.parseBoolean(v)), () -> onOff(m))).section("mine").visibleWhen(MyNameSettings::shown);
    }

    private static boolean shown() {
        return !ClassicUi.enabled() && MyRanksApi.available();
    }

    private static String style() {
        String s = Ranks.own().style();
        return s == null ? NONE : s;
    }

    private static void setStyle(String v) {
        MyRanksApi.setStyle(NONE.equals(v) ? null : v);
    }

    private static String rank() {
        String r = Ranks.own().primary();
        return r == null ? AUTO : r;
    }

    private static void setRank(String v) {
        MyRanksApi.setPrimary(AUTO.equals(v) ? null : v);
    }

    private static List<OptionSetting.Choice> styles(NametagsModule m) {
        OwnRanks own = Ranks.own();
        List<OptionSetting.Choice> out = new ArrayList<>();
        out.add(new OptionSetting.Choice(NONE, m.tr("mine.noStyle")));
        for (NameStyleDef s : MyStyles.allowed(own)) out.add(new OptionSetting.Choice(s.id(), s.label().get(Lang.german())));
        return out;
    }

    private static List<OptionSetting.Choice> ranks(NametagsModule m) {
        OwnRanks own = Ranks.own();
        List<OptionSetting.Choice> out = new ArrayList<>();
        out.add(new OptionSetting.Choice(AUTO, m.tr("mine.autoRank")));
        for (RankDef r : MyStyles.primaryRanks(own)) out.add(new OptionSetting.Choice(r.id(), r.label().get(Lang.german())));
        return out;
    }

    private static List<OptionSetting.Choice> onOff(NametagsModule m) {
        return List.of(new OptionSetting.Choice("true", m.tr("mine.shown")), new OptionSetting.Choice("false", m.tr("mine.hidden")));
    }
}
