package dev.henny.vibeyclient.module.social.vui;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.setting.OptionSetting;
import dev.henny.vibeyclient.module.social.SocialModule;
import dev.henny.vibeyclient.module.social.model.Model;
import dev.henny.vibeyclient.ui.vui.settings.Rows;
import dev.henny.vibeyclient.ui.vui.settings.SettingRow;

import java.util.ArrayList;
import java.util.List;

final class PrivacyRows {
    private static final String[] LEVELS = {"everyone", "friends_of_friends", "friends", "nobody"};
    private static final String[] FIELDS = {"friendRequests", "directMessages", "invites"};
    private static final String[] LABELS = {"ui.account.requests", "ui.account.messages", "ui.account.invites"};

    private PrivacyRows() {}

    static List<SettingRow> of(SocialModule module) {
        List<SettingRow> out = new ArrayList<>();
        for (int i = 0; i < FIELDS.length; i++) {
            String field = FIELDS[i];
            OptionSetting s = new OptionSetting(field, () -> value(module, field), v -> write(module, field, v), PrivacyRows::levels);
            out.add(Rows.of(s).labelled(Lang.tr(LABELS[i]), Lang.tr("social.privacy." + field)));
        }
        out.add(Rows.toggle("showHosting", Lang.tr("ui.account.hosting"), Lang.tr("ui.account.hostingTip"),
                () -> privacy(module) == null || privacy(module).showHosting(), v -> write(module, "showHosting", String.valueOf(v))));
        return out;
    }

    private static List<OptionSetting.Choice> levels() {
        List<OptionSetting.Choice> out = new ArrayList<>();
        for (String l : LEVELS) out.add(new OptionSetting.Choice(l, Lang.tr("ui.account.level." + l)));
        return out;
    }

    private static Model.Privacy privacy(SocialModule module) {
        Model.Me me = module.service().state.me;
        return me == null ? null : me.privacy();
    }

    private static String value(SocialModule module, String field) {
        Model.Privacy p = privacy(module);
        if (p == null) return "friends";
        return switch (field) {
            case "friendRequests" -> p.friendRequests();
            case "directMessages" -> p.directMessages();
            default -> p.invites();
        };
    }

    private static void write(SocialModule module, String field, String v) {
        JsonObject priv = new JsonObject();
        if (field.equals("showHosting")) priv.addProperty(field, Boolean.parseBoolean(v));
        else priv.addProperty(field, v);
        JsonObject body = new JsonObject();
        body.add("privacy", priv);
        module.service().updateMe(body);
    }
}
