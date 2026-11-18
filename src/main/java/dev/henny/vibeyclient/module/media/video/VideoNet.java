package dev.henny.vibeyclient.module.media.video;

import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.api.net.VibeyNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sharing screens with other Vibey Client players through the server channel ({@code vibeyclient:msg}, channel
 * {@code video:screen}). Only geometry, the http(s) URL and the playback state travel – never media bytes, never
 * local file paths. Owners resend their state every 10 s while playing and answer a newcomer's request; receivers
 * accept at most 8 screens and ~3 messages/s per player and filter URLs with {@link UrlPolicy}.
 */
public final class VideoNet {
    public static final String CHANNEL = "video:screen";
    static final int UPSERT = 1, STATE = 2, REMOVE = 3, REQUEST = 4;
    private static final int MAX_REMOTE_PER_PLAYER = 8;
    private static final long RESYNC_MS = 10_000;

    /** One message. {@code screen} only for UPSERT. */
    public record Msg(int op, UUID id, MediaScreen screen, boolean playing, long pos) {}

    static final StreamCodec<FriendlyByteBuf, Msg> CODEC = StreamCodec.of(VideoNet::encode, VideoNet::decode);

    private final Video video;
    private VibeyNet.Channel<Msg> channel;
    private final Set<UUID> sent = new HashSet<>();
    private final Map<UUID, float[]> buckets = new HashMap<>();
    private long lastResync;
    private long lastOnlineCheck;
    private boolean requestPending;
    private long answerAt;
    private final Set<UUID> askedBy = new HashSet<>();

    VideoNet(Video video) {
        this.video = video;
    }

    void register() {
        if (channel != null) return;
        channel = VibeyNet.channel(CHANNEL, CODEC).onReceive(this::receive);
        Events.subscribe(VibeyEvents.ServerHello.class, e -> {
            if (video.enabled()) {
                requestPending = true;
                resendAll();
            }
        });
    }

    void onJoin() {
        sent.clear();
        buckets.clear();
        askedBy.clear();
        requestPending = true;
    }

    void tick(long now) {
        if (channel == null || !VibeyNet.available()) return;
        if (requestPending) {
            requestPending = false;
            channel.broadcast(new Msg(REQUEST, new UUID(0, 0), null, false, 0));
        }
        if (answerAt != 0 && now >= answerAt) {
            answerAt = 0;
            for (UUID who : askedBy) for (MediaScreen s : video.screens.values()) if (shareable(s)) channel.sendTo(who, upsert(s));
            askedBy.clear();
        }
        if (now - lastResync > RESYNC_MS) {
            lastResync = now;
            for (MediaScreen s : video.screens.values()) if (shareable(s) && s.playing) sendState(s);
        }
        if (now - lastOnlineCheck > 5000) {
            lastOnlineCheck = now;
            var conn = Minecraft.getInstance().getConnection();
            if (conn != null) {
                Set<UUID> owners = new HashSet<>();
                for (MediaScreen s : video.screens.values()) if (s.remote) owners.add(s.owner);
                for (UUID o : owners) if (conn.getPlayerInfo(o) == null) video.forgetRemote(o);
            }
        }
    }

    private boolean shareable(MediaScreen s) {
        return !s.remote && s.placed() && s.shared && s.source != null && s.source.shareable();
    }

    private Msg upsert(MediaScreen s) {
        return new Msg(UPSERT, s.id, s, s.playing, s.expectedMs());
    }

    /** New or changed own screen (or sharing switched off: then it is removed for the others). */
    void sendScreen(MediaScreen s) {
        if (channel == null || !VibeyNet.available()) return;
        if (shareable(s)) {
            channel.broadcast(upsert(s));
            sent.add(s.id);
        } else if (sent.remove(s.id)) {
            channel.broadcast(new Msg(REMOVE, s.id, null, false, 0));
        }
    }

    void sendState(MediaScreen s) {
        if (channel == null || !VibeyNet.available() || !shareable(s)) return;
        if (!sent.contains(s.id)) {
            sendScreen(s);
            return;
        }
        channel.broadcast(new Msg(STATE, s.id, null, s.playing, s.expectedMs()));
    }

    void sendRemove(MediaScreen s) {
        if (channel == null || !VibeyNet.available()) return;
        if (sent.remove(s.id)) channel.broadcast(new Msg(REMOVE, s.id, null, false, 0));
    }

    private void resendAll() {
        for (MediaScreen s : video.screens.values()) if (shareable(s)) sendScreen(s);
    }

    // ============================================================================================ receive

    private boolean allow(UUID sender) {
        long now = System.currentTimeMillis();
        float[] b = buckets.computeIfAbsent(sender, k -> new float[] {6f, now});
        b[0] = Math.min(6f, b[0] + (now - (long) b[1]) / 1000f * 3f);
        b[1] = now;
        if (b[0] < 1f) return false;
        b[0] -= 1f;
        return true;
    }

    private void receive(UUID sender, Msg m) {
        if (!video.enabled() || sender == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && sender.equals(mc.player.getUUID())) return;
        if (!allow(sender)) return;
        switch (m.op()) {
            case UPSERT -> {
                if (video.cfg.others.get() == VideoSettings.Others.NEVER) return;
                MediaScreen s = m.screen();
                if (s == null || !UrlPolicy.acceptRemote(s.source)) return;
                if (!video.screens.containsKey(s.id) && video.countRemote(sender) >= MAX_REMOTE_PER_PLAYER) return;
                s.owner = sender;
                s.remote = true;
                s.shared = false;
                s.setTimeline(m.playing(), m.pos());
                PlayerInfo info = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(sender) : null;
                s.ownerName = info != null ? info.getProfile().name() : "?";
                video.upsertRemote(s);
            }
            case STATE -> video.remoteState(sender, m.id(), m.playing(), m.pos());
            case REMOVE -> video.remoteRemove(sender, m.id());
            case REQUEST -> {
                askedBy.add(sender);
                if (answerAt == 0) answerAt = System.currentTimeMillis() + 300 + (long) (Math.random() * 700);
            }
            default -> {
            }
        }
    }

    // ============================================================================================ codec

    private static void encode(FriendlyByteBuf buf, Msg m) {
        buf.writeByte(m.op());
        buf.writeUUID(m.id());
        switch (m.op()) {
            case UPSERT -> {
                MediaScreen s = m.screen();
                buf.writeUtf(s.dimension, 128);
                buf.writeDouble(s.x);
                buf.writeDouble(s.y);
                buf.writeDouble(s.z);
                buf.writeByte(s.normal.get3DDataValue());
                buf.writeByte(s.up.get3DDataValue());
                buf.writeFloat(s.width);
                buf.writeFloat(s.height);
                buf.writeByte(s.source.kind().ordinal());
                buf.writeUtf(s.source.uri(), MediaSource.MAX_URI);
                buf.writeBoolean(s.glowing);
                buf.writeBoolean(s.loop);
                buf.writeBoolean(m.playing());
                buf.writeVarLong(Math.max(0, m.pos()));
            }
            case STATE -> {
                buf.writeBoolean(m.playing());
                buf.writeVarLong(Math.max(0, m.pos()));
            }
            default -> {
            }
        }
    }

    private static Msg decode(FriendlyByteBuf buf) {
        int op = buf.readByte();
        UUID id = buf.readUUID();
        switch (op) {
            case UPSERT -> {
                MediaScreen s = new MediaScreen(id);
                s.dimension = buf.readUtf(128);
                s.x = finite(buf.readDouble(), 3.2e7);
                s.y = finite(buf.readDouble(), 4096);
                s.z = finite(buf.readDouble(), 3.2e7);
                s.normal = Direction.from3DDataValue(buf.readByte());
                s.up = Direction.from3DDataValue(buf.readByte());
                if (s.normal.getAxis() == s.up.getAxis()) throw new IllegalArgumentException("bad orientation");
                s.width = clamp(buf.readFloat());
                s.height = clamp(buf.readFloat());
                int kind = buf.readByte();
                MediaSource.Kind[] kinds = MediaSource.Kind.values();
                if (kind < 0 || kind >= kinds.length) throw new IllegalArgumentException("bad kind");
                s.source = new MediaSource(kinds[kind], buf.readUtf(MediaSource.MAX_URI));
                s.glowing = buf.readBoolean();
                s.loop = buf.readBoolean();
                boolean playing = buf.readBoolean();
                long pos = buf.readVarLong();
                return new Msg(op, id, s, playing, pos);
            }
            case STATE -> {
                boolean playing = buf.readBoolean();
                return new Msg(op, id, null, playing, buf.readVarLong());
            }
            case REMOVE, REQUEST -> {
                return new Msg(op, id, null, false, 0);
            }
            default -> throw new IllegalArgumentException("unknown op " + op);
        }
    }

    private static double finite(double v, double max) {
        if (!Double.isFinite(v) || Math.abs(v) > max) throw new IllegalArgumentException("bad coordinate");
        return v;
    }

    private static float clamp(float v) {
        if (!Float.isFinite(v)) throw new IllegalArgumentException("bad size");
        return Math.max(0.25f, Math.min(16f, v));
    }
}
