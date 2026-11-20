package dev.henny.vibeyclient.module.social;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.api.service.VibeBackend;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.backend.AuthStore;
import dev.henny.vibeyclient.backend.BackendClient;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.social.discord.DiscordPresence;
import dev.henny.vibeyclient.module.social.privacy.StreamerMode;
import dev.henny.vibeyclient.module.social.ui.AccountPanel;
import dev.henny.vibeyclient.module.social.ui.ConfirmScreen;
import dev.henny.vibeyclient.module.social.ui.SocialScreen;
import dev.henny.vibeyclient.api.service.FriendsDrawer;
import dev.henny.vibeyclient.api.service.SocialStatus;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.MenuEntries;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.VibeyTabs;
import dev.henny.vibeyclient.ui.toast.ToastHooks;
import dev.henny.vibeyclient.ui.toast.ToastStack;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.slf4j.Logger;

/**
 * Friends, requests, invites, chats and presence on top of the Vibe backend. Owns the backend connection
 * ({@link BackendClient}, registered as {@link VibeBackend} for other modules), the social state and the sidebar.
 */
public final class SocialModule extends VibeyModule {

    public final ActionSetting openFriends = action("openFriends", () -> openSidebar(null));
    public final KeybindSetting openKey = keybind("open", Keys.G).onPress(() -> openSidebar(null));
    public final EnumSetting<PresenceTracker.Status> status = choice("status", PresenceTracker.Status.ONLINE).section("status");
    public final BoolSetting autoAway = bool("autoAway", true).section("status");
    public final IntSetting autoAwayMinutes = intRange("autoAwayMinutes", 5, 1, 30).unit(" min").section("status");
    public final BoolSetting showPresence = bool("showPresence", true).section("privacy");
    public final BoolSetting hideServerAddress = bool("hideServerAddress", false).section("privacy");
    public final BoolSetting doNotDisturb = bool("doNotDisturb", false).section("notifications");
    public final BoolSetting toastFriendOnline = bool("toastFriendOnline", true).section("notifications");
    public final BoolSetting toastRequests = bool("toastRequests", true).section("notifications");
    public final BoolSetting toastMessages = bool("toastMessages", true).section("notifications");
    public final BoolSetting toastInvites = bool("toastInvites", true).section("notifications");
    public final BoolSetting toastSystem = bool("toastSystem", true).section("notifications");
    public final BoolSetting discord = bool("discord", true).section("discord");
    public final BoolSetting discordServer = bool("discordServer", false).section("discord");
    public final BoolSetting streamer = bool("streamer", false).section("streamer");
    public final BoolSetting streamerNames = bool("streamerNames", true).section("streamer");
    public final BoolSetting streamerSkins = bool("streamerSkins", true).section("streamer");
    public final BoolSetting streamerServer = bool("streamerServer", true).section("streamer");
    public final KeybindSetting streamerKey = keybind("streamerKey", Keys.F7).onPress(() -> this.streamerMode.toggle());

    private BackendClient backend;
    private SocialService service;
    private PresenceTracker presence;
    private InviteFlow invites;
    private final StreamerMode streamerMode = new StreamerMode(this);
    private final DiscordPresence discordPresence = new DiscordPresence(this);

    public SocialModule() {
        super("social", ModuleCategory.SOCIAL);
        autoAwayMinutes.visibleWhen(autoAway::on);
        for (BoolSetting b : new BoolSetting[]{toastFriendOnline, toastRequests, toastMessages, toastInvites, toastSystem}) {
            b.visibleWhen(() -> !doNotDisturb.on());
        }
        discordServer.visibleWhen(discord::on);
        streamerNames.visibleWhen(streamerMode::partPresent);
        streamerSkins.visibleWhen(streamerMode::partPresent);
    }

    /** Social needs the backend: hidden while online features are off. */
    @Override
    public boolean visible() {
        return GlobalSettings.online();
    }

    @Override
    public int accentColor() {
        return 0xFF3FE0C5;
    }

    // ============================================================================================ lifecycle

    @Override
    public void onLoad() {
        backend = new BackendClient(AuthStore.defaultStore());
        service = new SocialService(this, backend);
        presence = new PresenceTracker(this);
        invites = new InviteFlow(this);
        service.install();
        status.onChange(s -> presence.changed());
        showPresence.onChange(v -> presence.changed());
        hideServerAddress.onChange(v -> presence.changed());
        for (BoolSetting b : new BoolSetting[]{streamer, streamerNames, streamerSkins, streamerServer}) {
            b.onChange(v -> {
                streamerMode.apply();
                presence.changed();
            });
        }
        installStreamerPart();

        VibeyTabs.register(new VibeyTabs.Tab("account", "social.tab.account", 10, () -> new AccountPanel(this),
                () -> isEnabled() && GlobalSettings.online(), Icons.USER));

        on(VibeyEvents.OnlineFeaturesChanged.class, e -> apply());
        // "Friends" on the Vibey title screen and in the pause menu row opens the sidebar over that screen
        java.util.function.BooleanSupplier shown = () -> isEnabled() && GlobalSettings.online();
        MenuEntries.register(MenuEntries.Place.TITLE, new MenuEntries.Entry("friends", "core.title.friends", Icons.FRIENDS, 20,
                shown, this::openFriends, this::badge));
        MenuEntries.register(MenuEntries.Place.PAUSE, new MenuEntries.Entry("friends", "core.title.friends", Icons.FRIENDS, 20,
                shown, this::openFriends, this::badge));
        GlobalSettings.backendUrl.onChange(v -> dev.henny.vibeyclient.api.Tasks.debounce("vibeyclient-social-backend", 1500,
                () -> dev.henny.vibeyclient.api.Tasks.onMain(this::restart)));
        ToastHooks.addScreenKeyListener(this::screenKey);
        ToastStack.setHint(() -> tr("toast.hint", ClientCompat.keyName(openKey.mapping()).getString()));
    }

    @Override
    public void onEnable() {
        apply();
        streamerMode.apply();
    }

    @Override
    public void onDisable() {
        apply();
        streamerMode.apply();
        discordPresence.stop();
        if (ClientCompat.screen() instanceof SocialScreen s) ClientCompat.openScreen(s.parent());
    }

    @Override
    public void tick() {
        presence.tick();
        streamerMode.tick();
        discordPresence.tick();
    }

    private void installStreamerPart() {
        if (!streamerMode.partPresent()) return;
        try {
            Class.forName("dev.henny.vibeyclient.module.social.streamer.StreamerPart").getMethod("install").invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            log.error("Could not install streamer mode", e);
        }
    }

    /** Connects when the module and online features are on, disconnects otherwise (keeps the stored login). */
    private void apply() {
        boolean want = isEnabled() && GlobalSettings.online();
        if (want) {
            if (!backend.running()) backend.start(true);
            Services.register(VibeBackend.class, backend);
            Services.register(SocialStatus.class, socialStatus);
            Services.register(FriendsDrawer.class, drawer);
        } else {
            Services.unregister(VibeBackend.class, backend);
            Services.unregister(SocialStatus.class, socialStatus);
            Services.unregister(FriendsDrawer.class, drawer);
            if (backend.running()) backend.stop();
        }
    }

    private final FriendsDrawer drawer = new dev.henny.vibeyclient.module.social.vui.SocialDrawer(this);

    private void openFriends(Screen parent) {
        if (dev.henny.vibeyclient.ui.vui.ClassicUi.enabled()) ClientCompat.openScreen(new SocialScreen(parent, this, null));
        else dev.henny.vibeyclient.ui.vui.Menus.openFriends(parent);
    }

    /** Numbers for the title screen status line and the menu badges (render thread, cheap). */
    private final SocialStatus socialStatus = new SocialStatus() {
        @Override
        public int onlineFriends() {
            return service.ready() ? service.state.onlineCount() : -1;
        }

        @Override
        public int badge() {
            return SocialModule.this.badge();
        }
    };

    /** Unread chats + incoming requests + open invites. */
    public int badge() {
        if (!service.ready()) return 0;
        SocialState st = service.state;
        return st.unreadChats() + st.incomingRequests() + st.openIncomingInviteCount();
    }

    private void restart() {
        if (backend.running()) {
            backend.stop();
            apply();
        }
    }

    // ============================================================================================ API for the UI

    public BackendClient backend() {
        return backend;
    }

    public SocialService service() {
        return service;
    }

    public PresenceTracker presence() {
        return presence;
    }

    public InviteFlow invites() {
        return invites;
    }

    public StreamerMode streamerMode() {
        return streamerMode;
    }

    public DiscordPresence discordPresence() {
        return discordPresence;
    }

    public boolean dnd() {
        return doNotDisturb.on();
    }

    public boolean showHosting() {
        var me = service.state.me;
        return me == null || me.privacy().showHosting();
    }

    public void setStatus(PresenceTracker.Status s) {
        status.set(s);
    }

    public void login() {
        if (!backend.running()) apply();
        backend.login();
    }

    public void logout() {
        backend.logout();
    }

    public Logger log() {
        return log;
    }

    public void logError(String what, Throwable t) {
        log.error("Social {} failed", what, t);
    }

    /** Opens the social sidebar over the current screen (or shows {@code route} in the open one). */
    public void openSidebar(SidebarRoute route) {
        if (!isEnabled() || !GlobalSettings.online()) return;
        Screen cur = ClientCompat.screen();
        if (cur instanceof SocialScreen s) {
            if (route != null) s.show(route);
            return;
        }
        if (cur instanceof ConfirmScreen) return;
        ClientCompat.openScreen(new SocialScreen(cur, this, route));
    }

    // ============================================================================================ menus

    /** The social key over menus where it cannot mean anything else. */
    private boolean screenKey(Screen screen, int key) {
        if (!isEnabled() || !GlobalSettings.online()) return false;
        if (!ClientCompat.matches(openKey.mapping(), key)) return false;
        boolean safe = screen instanceof TitleScreen || screen instanceof PauseScreen || screen instanceof JoinMultiplayerScreen
                || screen instanceof SelectWorldScreen || screen instanceof VibeyScreen
                || screen instanceof dev.henny.vibeyclient.ui.vui.menu.MenuScreen;
        if (!safe) return false;
        GuiEventListener f = screen.getFocused();
        if (f instanceof EditBox eb && eb.isFocused()) return false;
        openSidebar(null);
        return true;
    }
}
