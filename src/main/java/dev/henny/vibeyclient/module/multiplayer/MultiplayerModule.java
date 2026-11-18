package dev.henny.vibeyclient.module.multiplayer;

import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.api.service.HostingService;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.api.setting.Settings.StringSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.multiplayer.host.HostConfig;
import dev.henny.vibeyclient.module.multiplayer.host.HostSession;
import dev.henny.vibeyclient.module.multiplayer.screen.HostScreen;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Host your singleplayer world for friends through the Vibey relay (port of Vibey Multiplayer, both loaders).
 * Conflicts with the standalone mod "vibey": then this module is off, registers nothing and its mixins are skipped.
 *
 * <p>The simple host options are typed settings (Vibey menu); the whitelist and ban lists live in the same file
 * ({@link HostConfig}). While enabled the module provides {@link HostingService}.
 */
public final class MultiplayerModule extends VibeyModule {
    public static final Logger LOG = LoggerFactory.getLogger("VibeyClient/multiplayer");
    private static MultiplayerModule instance;

    public enum GameMode { SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR }

    public final ActionSetting openHost = action("openHost", this::openHostScreen);
    public final EnumSetting<GameMode> gameMode = choice("gameMode", GameMode.SURVIVAL);
    public final BoolSetting cheats = bool("cheats", false);
    public final BoolSetting pvp = bool("pvp", true);
    public final IntSetting maxPlayers = intRange("maxPlayers", 8, 2, 8);
    public final BoolSetting forceMods = bool("forceMods", false);
    public final BoolSetting pauseButton = bool("pauseButton", true);
    public final BoolSetting toasts = bool("toasts", true);
    public final KeybindSetting hostKey = keybind("host", Keys.UNBOUND).onPress(this::openHostScreen);
    public final BoolSetting exactModVersions = bool("exactModVersions", false).advanced();
    public final StringSetting relay = string("relay", BuildInfo.DEFAULT_RELAY, 253).advanced();
    public final IntSetting lanPort = intRange("lanPort", 0, 0, 65535).field().advanced();

    private final HostingService service = new Service();

    public MultiplayerModule() {
        super("multiplayer", ModuleCategory.MULTIPLAYER);
    }

    public static MultiplayerModule instance() {
        return instance;
    }

    @Override
    public int accentColor() {
        return 0xFF3FE0C5;
    }

    // ============================================================================================ lifecycle

    @Override
    public void onLoad() {
        instance = this;
        HostConfig.load(this);
        // Changes in the Vibey menu go into the host config (and live into a running session).
        gameMode.onChange(v -> {
            String name = v.name().toLowerCase(Locale.ROOT);
            if (!name.equals(HostConfig.get().gameMode)) {
                HostConfig.get().gameMode = name;
                HostConfig.save();
                HostSession.applyGameMode();
            }
        });
        cheats.onChange(v -> {
            if (v != HostConfig.get().cheats) {
                HostConfig.get().cheats = v;
                HostConfig.save();
                HostSession.applyCheats();
            }
        });
        pvp.onChange(v -> {
            if (v != HostConfig.get().pvp) {
                HostConfig.get().pvp = v;
                HostConfig.save();
                HostSession.applyPvp();
            }
        });
        maxPlayers.onChange(v -> sync());
        forceMods.onChange(v -> sync());
        exactModVersions.onChange(v -> sync());
        relay.onChange(v -> sync());
        lanPort.onChange(v -> sync());

        MpLoader.register();
        on(VibeyEvents.ScreenInit.class, e -> {
            if (pauseButton.get()) MpPauseMenu.decorate(e.screen(), e.widgets(), e.addWidget(), e.removeWidget());
        });
    }

    @Override
    public void onEnable() {
        Services.register(HostingService.class, service);
    }

    @Override
    public void onDisable() {
        if (HostSession.isHosting()) HostSession.stop();
        Services.unregister(HostingService.class, service);
    }

    @Override
    public void onWorldJoin() {
        MpLoader.sendModList();
    }

    // ============================================================================================ settings <-> config

    private void sync() {
        HostConfig c = HostConfig.get();
        if (c.maxPlayers == maxPlayers.get() && c.forceMods == forceMods.get() && c.exactModVersions == exactModVersions.get()
                && c.relay.equals(relay.get().trim()) && c.lanPort == lanPort.get()) return;
        copySettingsInto(c);
        HostConfig.save();
    }

    /** Typed settings -> config fields. */
    public void copySettingsInto(HostConfig c) {
        c.gameMode = gameMode.get().name().toLowerCase(Locale.ROOT);
        c.cheats = cheats.get();
        c.pvp = pvp.get();
        c.maxPlayers = maxPlayers.get();
        c.forceMods = forceMods.get();
        c.exactModVersions = exactModVersions.get();
        c.relay = relay.get().isBlank() ? BuildInfo.DEFAULT_RELAY : relay.get().trim();
        c.lanPort = lanPort.get();
    }

    /** Config fields -> typed settings (after the host screens changed something). */
    public void copySettingsFrom(HostConfig c) {
        for (GameMode m : GameMode.values()) if (m.name().equalsIgnoreCase(c.gameMode)) gameMode.set(m);
        cheats.set(c.cheats);
        pvp.set(c.pvp);
        maxPlayers.set(c.maxPlayers);
        forceMods.set(c.forceMods);
        exactModVersions.set(c.exactModVersions);
        relay.set(c.relay);
        lanPort.set(c.lanPort);
    }

    // ============================================================================================ ui

    public void openHostScreen() {
        if (!isEnabled()) return;
        ClientCompat.openScreen(new HostScreen(ClientCompat.screen()));
    }

    /** A toast (if the player wants them). Any thread. */
    public static void toast(String title, String body, boolean success) {
        MultiplayerModule m = instance;
        if (m == null || !m.toasts.get()) return;
        Notify.show(new Notify.Toast(title, body, success ? Notify.Kind.SUCCESS : Notify.Kind.ERROR, null,
                m::openHostScreen));
    }

    /** "Steve joined your world" with the player's head. Any thread. */
    public static void guestToast(String title, String uuid) {
        MultiplayerModule m = instance;
        if (m == null || !m.toasts.get()) return;
        Notify.show(new Notify.Toast(title, T.get("toast.guest.body"), Notify.Kind.SOCIAL, uuid, m::openHostScreen));
    }

    // ============================================================================================ service

    private final class Service implements HostingService {
        @Override
        public boolean canHost() {
            return isEnabled() && HostSession.canHost();
        }

        @Override
        public boolean isHosting() {
            return HostSession.isHosting();
        }

        @Override
        public String address() {
            if (!HostSession.isHosting() || HostSession.status() != HostSession.Status.ONLINE) return null;
            String a = HostSession.address();
            int colon = a.lastIndexOf(':');
            return colon > 0 ? a.substring(0, colon) : a;
        }

        @Override
        public int port() {
            String a = HostSession.address();
            int colon = a.lastIndexOf(':');
            if (colon > 0) {
                try {
                    return Integer.parseInt(a.substring(colon + 1));
                } catch (NumberFormatException ignored) {
                }
            }
            return 25565;
        }

        @Override
        public void startHosting() {
            Minecraft.getInstance().execute(() -> {
                if (canHost()) HostSession.start();
            });
        }

        @Override
        public void stopHosting() {
            Minecraft.getInstance().execute(HostSession::stop);
        }

        @Override
        public void openHostScreen() {
            Minecraft.getInstance().execute(MultiplayerModule.this::openHostScreen);
        }
    }
}
