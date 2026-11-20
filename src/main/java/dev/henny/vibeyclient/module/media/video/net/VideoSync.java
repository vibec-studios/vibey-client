package dev.henny.vibeyclient.module.media.video.net;

import dev.henny.r3.persist.PlacementCodec;
import dev.henny.r3.sync.SharedObjects;
import dev.henny.r3.sync.SyncPolicy;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.module.media.video.MediaScreen;
import dev.henny.vibeyclient.module.media.video.ScreenData;
import dev.henny.vibeyclient.module.media.video.UrlPolicy;
import dev.henny.vibeyclient.module.media.video.Video;
import dev.henny.vibeyclient.module.media.video.VideoFeature;
import dev.henny.vibeyclient.module.media.video.world.ScreenObjects;
import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class VideoSync {
    private static final long RESYNC_MS = 10_000;
    private static final long ONLINE_CHECK_MS = 5_000;

    private final Video video;
    private final JsonChannel channel = new JsonChannel();
    private final RemoteScreens remote;
    private SharedObjects shared;
    private boolean requestPending;
    private long lastResync;
    private long lastOnlineCheck;

    public VideoSync(Video video) {
        this.video = video;
        this.remote = new RemoteScreens(video);
    }

    public void register() {
        channel.register();
        Events.subscribe(VibeyEvents.ServerHello.class, e -> {
            if (!video.inWorld()) return;
            requestPending = true;
            for (MediaScreen s : video.screens.values()) sendScreen(s);
        });
    }

    public void onJoin() {
        Minecraft mc = Minecraft.getInstance();
        UUID self = mc.player != null ? mc.player.getUUID() : new UUID(0, 0);
        SyncPolicy policy = SyncPolicy.defaults().accepting(r ->
                video.cfg.others.get() != VideoFeature.Others.NEVER && UrlPolicy.acceptRemote(ScreenData.source(r.data)));
        shared = new SharedObjects(ScreenObjects.KIND, channel, policy, remote, self, System::currentTimeMillis, System.nanoTime());
        requestPending = true;
    }

    public void onLeave() {
        if (shared != null) shared.clear();
        shared = null;
        channel.receiver(null);
    }

    public void tick(long now) {
        if (shared == null || !channel.available()) return;
        if (requestPending) {
            requestPending = false;
            shared.requestAll();
        }
        shared.tick();
        if (now - lastResync > RESYNC_MS) {
            lastResync = now;
            for (MediaScreen s : video.screens.values()) if (shareable(s) && s.playing()) sendState(s);
        }
        if (now - lastOnlineCheck > ONLINE_CHECK_MS) {
            lastOnlineCheck = now;
            forgetLeavers();
        }
    }

    private void forgetLeavers() {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) return;
        Set<UUID> owners = new HashSet<>();
        for (MediaScreen s : video.screens.values()) if (s.remote) owners.add(s.owner);
        for (UUID o : owners) if (conn.getPlayerInfo(o) == null) shared.forgetOwner(o);
    }

    private static boolean shareable(MediaScreen s) {
        return !s.remote && s.placed() && s.shared && s.source != null && s.source.shareable();
    }

    public void sendScreen(MediaScreen s) {
        if (shared == null) return;
        if (shareable(s)) shared.publish(PlacementCodec.encode(s.object, ScreenData.encode(s, true)));
        else if (shared.published().containsKey(s.id.toString())) shared.unpublish(s.id.toString());
    }

    public void sendState(MediaScreen s) {
        if (shared == null || !shareable(s)) return;
        if (!shared.published().containsKey(s.id.toString())) sendScreen(s);
        else shared.publishState(s.id.toString(), ScreenData.state(s));
    }

    public void sendRemove(MediaScreen s) {
        if (shared != null) shared.unpublish(s.id.toString());
    }
}
