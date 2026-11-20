package dev.henny.vibeyclient.module.media.video.net;

import com.google.gson.JsonObject;
import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.persist.PlacementCodec;
import dev.henny.r3.persist.PlacementRecord;
import dev.henny.r3.sync.SharedObjectsListener;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.module.media.video.MediaScreen;
import dev.henny.vibeyclient.module.media.video.MediaSource;
import dev.henny.vibeyclient.module.media.video.ScreenData;
import dev.henny.vibeyclient.module.media.video.Video;
import dev.henny.vibeyclient.module.media.video.world.ScreenObjects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.Objects;
import java.util.UUID;

final class RemoteScreens implements SharedObjectsListener {
    private final Video video;

    RemoteScreens(Video video) {
        this.video = video;
    }

    @Override
    public void upserted(UUID owner, PlacementRecord record, boolean created) {
        try {
            UUID id = canonical(record.id);
            if (id == null) return;
            MediaScreen s = video.screens.get(id);
            if (s != null && (!s.remote || !owner.equals(s.owner))) return;
            if (s == null) create(owner, id, record);
            else update(s, record);
        } catch (RuntimeException e) {
            Vibey.LOG.debug("Dropped a shared video screen: {}", e.toString());
        }
    }

    private void create(UUID owner, UUID id, PlacementRecord record) {
        MediaScreen s = new MediaScreen(id);
        s.remote = true;
        s.shared = false;
        s.owner = owner;
        s.ownerName = name(owner);
        ScreenData.apply(record.data, s, true);
        s.glowing = record.fullBright;
        WorldObject o = PlacementCodec.decode(record, ScreenObjects.LIMITS);
        ScreenObjects.dress(o, s);
        R3Mc.add(o);
        video.screens.put(id, s);
    }

    private void update(MediaScreen s, PlacementRecord record) {
        MediaSource before = s.source;
        ScreenData.apply(record.data, s, true);
        s.glowing = record.fullBright;
        s.ownerName = name(s.owner);
        if (!Objects.equals(before, s.source)) {
            s.accepted = false;
            s.timeline.durationMs(-1);
            video.players.close(s);
        }
        if (s.object != null) {
            PlacementCodec.apply(record, s.object, ScreenObjects.LIMITS);
            ScreenObjects.dress(s.object, s);
        }
    }

    @Override
    public void state(UUID owner, String id, JsonObject payload) {
        MediaScreen s = own(owner, id);
        if (s == null || payload == null) return;
        try {
            s.setTimeline(payload.get("playing").getAsBoolean(), payload.get("pos").getAsLong());
        } catch (RuntimeException e) {
            Vibey.LOG.debug("Bad video state from {}: {}", owner, e.toString());
        }
    }

    @Override
    public void removed(UUID owner, String id) {
        MediaScreen s = own(owner, id);
        if (s != null) video.actions.remove(s);
    }

    private MediaScreen own(UUID owner, String id) {
        UUID uuid = canonical(id);
        MediaScreen s = uuid != null ? video.screens.get(uuid) : null;
        return s != null && s.remote && owner.equals(s.owner) ? s : null;
    }

    static UUID canonical(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            return uuid.toString().equals(id) ? uuid : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String name(UUID owner) {
        var conn = Minecraft.getInstance().getConnection();
        PlayerInfo info = conn != null ? conn.getPlayerInfo(owner) : null;
        return info != null ? info.getProfile().name() : "?";
    }
}
