package dev.henny.vibeyclient.module.social;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.api.service.VibeBackend;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.backend.AuthStore;
import dev.henny.vibeyclient.backend.BackendClient;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.social.ui.AccountPanel;
import dev.henny.vibeyclient.module.social.ui.ConfirmScreen;
import dev.henny.vibeyclient.module.social.ui.SocialScreen;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.VibeyTabs;
import dev.henny.vibeyclient.ui.toast.ToastHooks;
import dev.henny.vibeyclient.ui.toast.ToastStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.slf4j.Logger;

/**
 * Friends, requests, invites, chats and presence on top of the Vibe backend. Owns the backend connection
 * ({@link BackendClient}, registered as {@link VibeBackend} for other modules), the social state and the sidebar.
 */
public final class SocialModule extends VibeyModule {

    public final KeybindSetting openKey = keybind("open", Keys.G).onPress(() -> openSidebar(null));
    public final EnumSetting<PresenceTracker.Status> status = choice("status", PresenceTracker.Status.ONLINE);
    public final BoolSetting autoAway = bool("autoAway", true);
    public final IntSetting autoAwayMinutes = intRange("autoAwayMinutes", 5, 1, 30).unit(" min");
    public final BoolSetting showPresence = bool("showPresence", true);
    public final BoolSetting hideServerAddress = bool("hideServerAddress", false);
    public final BoolSetting doNotDisturb = bool("doNotDisturb", false);
    public final BoolSetting toastFriendOnline = bool("toastFriendOnline", true);
    public final BoolSetting toastRequests = bool("toastRequests", true);
    public final BoolSetting toastMessages = bool("toastMessages", true);
    public final BoolSetting toastInvites = bool("toastInvites", true);
    public final BoolSetting toastSystem = bool("toastSystem", true);

    private BackendClient backend;
    private SocialService service;
    private PresenceTracker presence;
    private InviteFlow invites;

    public SocialModule() {
        super("social", ModuleCategory.SOCIAL);
        autoAwayMinutes.visibleWhen(autoAway::on);
        for (BoolSetting b : new BoolSetting[]{toastFriendOnline, toastRequests, toastMessages, toastInvites, toastSystem}) {
            b.visibleWhen(() -> !doNotDisturb.on());
        }
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

        VibeyTabs.register(new VibeyTabs.Tab("account", "social.tab.account", 10, () -> new AccountPanel(this),
                () -> isEnabled() && GlobalSettings.online()));

        on(VibeyEvents.OnlineFeaturesChanged.class, e -> apply());
        on(VibeyEvents.ScreenInit.class, e -> decorate(e));
        GlobalSettings.backendUrl.onChange(v -> dev.henny.vibeyclient.api.Tasks.debounce("vibeyclient-social-backend", 1500,
                () -> dev.henny.vibeyclient.api.Tasks.onMain(this::restart)));
        ToastHooks.addScreenKeyListener(this::screenKey);
        ToastStack.setHint(() -> tr("toast.hint", ClientCompat.keyName(openKey.mapping()).getString()));
    }

    @Override
    public void onEnable() {
        apply();
    }

    @Override
    public void onDisable() {
        apply();
        if (ClientCompat.screen() instanceof SocialScreen s) ClientCompat.openScreen(s.parent());
    }

    @Override
    public void tick() {
        presence.tick();
    }

    /** Connects when the module and online features are on, disconnects otherwise (keeps the stored login). */
    private void apply() {
        boolean want = isEnabled() && GlobalSettings.online();
        if (want) {
            if (!backend.running()) backend.start(true);
            Services.register(VibeBackend.class, backend);
        } else {
            Services.unregister(VibeBackend.class, backend);
            if (backend.running()) backend.stop();
        }
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
                || screen instanceof SelectWorldScreen || screen instanceof VibeyScreen;
        if (!safe) return false;
        GuiEventListener f = screen.getFocused();
        if (f instanceof EditBox eb && eb.isFocused()) return false;
        openSidebar(null);
        return true;
    }

    private void decorate(VibeyEvents.ScreenInit e) {
        if (!(e.screen() instanceof PauseScreen) || !GlobalSettings.online()) return;
        boolean full = false;
        for (GuiEventListener l : e.widgets()) {
            if (l instanceof Button b && b.getMessage().getContents() instanceof TranslatableContents tc && tc.getKey().equals("menu.returnToGame")) {
                full = true;
            }
        }
        if (!full) return;
        int unread = service.state.unreadChats() + service.state.incomingRequests() + service.state.openIncomingInvites().size();
        String label = tr("pause.button") + (unread > 0 ? " (" + unread + ")" : "");
        e.addWidget().accept(Button.builder(Lang.text("social.pause.button").copy().append(unread > 0 ? " (" + unread + ")" : ""),
                b -> openSidebar(null)).bounds(74, 6, Math.max(64, net.minecraft.client.Minecraft.getInstance().font.width(label) + 12), 20).build());
    }
}
