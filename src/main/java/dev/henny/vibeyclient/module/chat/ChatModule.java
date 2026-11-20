package dev.henny.vibeyclient.module.chat;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.privacy.Privacy;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.api.setting.Settings.StringSetting;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.chat.core.HistorySize;
import dev.henny.vibeyclient.module.chat.core.TimeStyle;
import dev.henny.vibeyclient.module.chat.filters.FilterScreens;
import dev.henny.vibeyclient.module.chat.filters.FilterStore;
import dev.henny.vibeyclient.module.chat.hook.ChatRefresh;
import dev.henny.vibeyclient.module.chat.notify.ChatToasts;
import dev.henny.vibeyclient.module.chat.screen.CopyButton;
import dev.henny.vibeyclient.module.chat.screen.SearchMode;
import dev.henny.vibeyclient.module.chat.window.ChatWindow;
import dev.henny.vibeyclient.ui.Icons;

public final class ChatModule extends VibeyModule {
    private static volatile ChatModule instance;

    public final ActionSetting editFilters = action("editFilters", FilterScreens::open);

    public final BoolSetting timestamps = bool("timestamps", true).section("lines");
    public final EnumSetting<TimeStyle> timeStyle = choice("timeStyle", TimeStyle.H24).section("lines");
    public final BoolSetting copyButton = bool("copyButton", true).section("lines");
    public final BoolSetting movable = bool("movable", true).section("lines");
    public final IntSetting history = intRange("history", 500, HistorySize.MIN, HistorySize.MAX, 100).section("lines");
    public final BoolSetting keepOnLeave = bool("keepOnLeave", false).section("lines");

    public final BoolSetting highlight = bool("highlight", true).section("mentions");
    public final StringSetting mentionWords = string("mentionWords", "", 120).section("mentions");
    public final BoolSetting mentionToast = bool("mentionToast", true).section("mentions");
    public final BoolSetting dmToast = bool("dmToast", true).section("mentions");
    public final BoolSetting mentionSound = bool("mentionSound", true).section("mentions");

    public final BoolSetting collapse = bool("collapse", true).section("filter");
    public final BoolSetting filters = bool("filters", true).section("filter");

    public final KeybindSetting searchKey = keybind("searchKey", Keys.UNBOUND).onPress(SearchMode::openChat);

    private final FilterStore filterStore = new FilterStore(this);

    public ChatModule() {
        super("chat", ModuleCategory.SOCIAL);
        instance = this;
        timeStyle.visibleWhen(timestamps::on);
    }

    public static ChatModule get() {
        return instance;
    }

    public static boolean on() {
        ChatModule m = instance;
        return m != null && m.isEnabled();
    }

    public FilterStore filterStore() {
        return filterStore;
    }

    @Override
    public String icon() {
        return Icons.CHAT;
    }

    @Override
    public int accentColor() {
        return 0xFF8B7CFF;
    }

    @Override
    public void onLoad() {
        filterStore.load();
        CopyButton.register();
        ChatWindow.register(this);
        Privacy.listen(ChatRefresh::later);
        Privacy.listen(() -> Tasks.onMain(ChatToasts::privacyChanged));
        for (BoolSetting b : new BoolSetting[] {timestamps, highlight, collapse}) b.onChange(v -> ChatRefresh.later());
        timeStyle.onChange(v -> ChatRefresh.later());
        mentionWords.onChange(v -> ChatRefresh.later());
        history.onChange(v -> ChatRefresh.later());
        movable.onChange(v -> ChatWindow.shown(v));
    }

    @Override
    public void onEnable() {
        ChatRefresh.later();
    }

    @Override
    public void onDisable() {
        SearchMode.exit();
        ChatRefresh.later();
    }
}
