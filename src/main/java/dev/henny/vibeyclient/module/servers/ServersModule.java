package dev.henny.vibeyclient.module.servers;

import com.google.gson.JsonElement;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.module.servers.core.Favourites;
import dev.henny.vibeyclient.module.servers.ping.PingStore;
import dev.henny.vibeyclient.module.servers.screen.ServerScreenHooks;

public final class ServersModule extends VibeyModule {
    private static final String FAVOURITES = "favourites";
    private static volatile ServersModule instance;

    public final BoolSetting search = bool("search", true).section("list");
    public final BoolSetting favourites = bool("favourites", true).section("list");
    public final BoolSetting favouritesFirst = bool("favouritesFirst", true).section("list").visibleWhen(this::favOn);
    public final BoolSetting motdPreview = bool("motdPreview", true).section("list");

    public final BoolSetting livePing = bool("livePing", true).section("ping");
    public final IntSetting pingEvery = intRange("pingEvery", 30, 10, 120, 5).unit("s").section("ping").visibleWhen(this::liveOn);
    public final IntSetting pingParallel = intRange("pingParallel", 2, 1, 4).section("ping").advanced().visibleWhen(this::liveOn);

    private final Favourites favs = new Favourites();

    public ServersModule() {
        super("servers", ModuleCategory.MULTIPLAYER);
        instance = this;
    }

    public static ServersModule get() {
        return instance;
    }

    @Override
    public int accentColor() {
        return 0xFF3FE0C5;
    }

    @Override
    public void onLoad() {
        JsonElement e = settings().data().get(FAVOURITES);
        favs.read(e);
        pingEvery.onChange(s -> PingStore.ttl(s));
        PingStore.ttl(pingEvery.get());
        ServerScreenHooks.register(this);
    }

    public Favourites favs() {
        return favs;
    }

    public boolean toggleFavourite(String address) {
        boolean on = favs.toggle(address);
        settings().data().add(FAVOURITES, favs.write());
        settings().markDirty();
        return on;
    }

    public boolean favourite(String address) {
        return favouritesActive() && favs.contains(address);
    }

    private boolean favOn() {
        return favourites.on();
    }

    private boolean liveOn() {
        return livePing.on();
    }

    public boolean searchActive() {
        return isEnabled() && search.on();
    }

    public boolean favouritesActive() {
        return isEnabled() && favourites.on();
    }

    public boolean previewActive() {
        return isEnabled() && motdPreview.on();
    }

    public boolean liveActive() {
        return isEnabled() && livePing.on();
    }
}
