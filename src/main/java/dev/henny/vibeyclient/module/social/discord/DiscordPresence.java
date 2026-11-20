package dev.henny.vibeyclient.module.social.discord;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.privacy.Privacy;
import dev.henny.vibeyclient.api.service.HostingService;
import dev.henny.vibeyclient.module.social.SocialCompat;
import dev.henny.vibeyclient.module.social.SocialModule;
import dev.henny.vibeyclient.module.social.discord.core.RpcInputs;
import dev.henny.vibeyclient.module.social.discord.core.RpcLabels;
import dev.henny.vibeyclient.module.social.discord.core.RpcPayload;
import dev.henny.vibeyclient.module.social.discord.core.RpcPlace;
import dev.henny.vibeyclient.module.social.discord.ipc.Connectors;

public final class DiscordPresence {
    private static final int EVERY_TICKS = 40;

    private final SocialModule module;
    private DiscordWorker worker;
    private int ticks;
    private String placeKey = "";
    private long startedAt = System.currentTimeMillis();

    public DiscordPresence(SocialModule module) {
        this.module = module;
    }

    public static String appId() {
        String override = System.getProperty("vibeyclient.discordAppId");
        return override != null ? override.strip() : BuildInfo.DISCORD_APP_ID;
    }

    public boolean connected() {
        return worker != null && worker.connected();
    }

    public void tick() {
        if (++ticks % EVERY_TICKS != 0) return;
        boolean want = module.isEnabled() && GlobalSettings.online() && module.discord.on() && !appId().isEmpty();
        if (!want) {
            stop();
            return;
        }
        if (worker == null) worker = new DiscordWorker(Connectors.local(), appId(), module.log()::debug);
        worker.push(activity());
    }

    public void stop() {
        if (worker == null) return;
        worker.stop();
        worker = null;
    }

    private JsonObject activity() {
        RpcPlace place = place();
        String server = place == RpcPlace.MULTIPLAYER ? SocialCompat.serverAddress() : null;
        String key = place + "|" + server + "|" + SocialCompat.singleplayerWorld();
        if (!key.equals(placeKey)) {
            placeKey = key;
            startedAt = System.currentTimeMillis();
        }
        RpcInputs in = new RpcInputs(module.showPresence.on(), module.presence().effectiveStatus(), place, server,
                module.discordServer.on() && !module.hideServerAddress.on() && !ServerRules.isDisabled("discord.server"),
                Privacy.hideServer(), startedAt,
                "Vibey Client " + BuildInfo.VERSION + " · " + BuildInfo.MINECRAFT);
        return RpcPayload.build(in, labels());
    }

    private static RpcPlace place() {
        if (SocialCompat.singleplayerWorld() != null) {
            HostingService hs = Services.get(HostingService.class).orElse(null);
            return hs != null && hs.isHosting() ? RpcPlace.HOSTING : RpcPlace.SINGLEPLAYER;
        }
        return SocialCompat.serverAddress() != null ? RpcPlace.MULTIPLAYER : RpcPlace.MENU;
    }

    private static RpcLabels labels() {
        return new RpcLabels(Lang.tr("social.discord.menu"), Lang.tr("social.discord.singleplayer"),
                Lang.tr("social.discord.multiplayer"), Lang.tr("social.discord.hosting"),
                Lang.tr("social.discord.away"), Lang.tr("social.discord.busy"));
    }
}
