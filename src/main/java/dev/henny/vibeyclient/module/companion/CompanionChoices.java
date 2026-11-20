package dev.henny.vibeyclient.module.companion;

import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.def.VariantDef;
import dev.henny.companio.mc.CompanioMc;
import dev.henny.companio.world.Command;
import dev.henny.vibeyclient.api.setting.OptionSetting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

final class CompanionChoices {
    static final String NONE = "none";

    private CompanionChoices() {}

    private static String lang() {
        return Minecraft.getInstance().options.languageCode;
    }

    static String selected() {
        if (!CompanioMc.ready()) return NONE;
        return CompanioMc.config().enabled && !CompanioMc.config().selected.isEmpty() ? CompanioMc.config().selected : NONE;
    }

    static void select(String key) {
        CompanioMc.select(NONE.equals(key) ? "" : key);
    }

    static List<OptionSetting.Choice> companions(CompanionModule m) {
        List<OptionSetting.Choice> out = new ArrayList<>();
        out.add(new OptionSetting.Choice(NONE, m.tr("none")));
        for (CompanionDef d : CompanioMc.companions()) out.add(new OptionSetting.Choice(d.key(), d.name().resolve(d.lang(), lang())));
        return out;
    }

    static CompanionDef current() {
        if (!CompanioMc.ready()) return null;
        String key = CompanioMc.config().selected;
        for (CompanionDef d : CompanioMc.companions()) if (d.key().equals(key)) return d;
        return null;
    }

    static boolean hasVariants() {
        CompanionDef d = current();
        return d != null && d.variants().size() > 1;
    }

    static boolean sizeAdjustable() {
        CompanionDef d = current();
        return d != null && d.size().adjustable();
    }

    static String variant() {
        CompanionDef d = current();
        if (d == null) return "";
        String v = CompanioMc.config().variant;
        return v.isEmpty() ? d.defaultVariant() : v;
    }

    static void setVariant(String v) {
        CompanioMc.config().variant = v;
        CompanioMc.changed();
    }

    static List<OptionSetting.Choice> variants(CompanionModule m) {
        List<OptionSetting.Choice> out = new ArrayList<>();
        CompanionDef d = current();
        if (d != null) for (VariantDef v : d.variants()) out.add(new OptionSetting.Choice(v.id(), v.name().resolve(d.lang(), lang())));
        return out;
    }

    static String command() {
        return CompanioMc.ready() ? CompanioMc.config().command : Command.FOLLOW.id();
    }

    static List<OptionSetting.Choice> commands(CompanionModule m) {
        List<OptionSetting.Choice> out = new ArrayList<>();
        for (Command c : Command.values()) out.add(new OptionSetting.Choice(c.id(), m.tr("mode." + c.id())));
        return out;
    }
}
