package dev.henny.vibeyclient.module.media.video.net;

import dev.henny.r3.sync.SyncChannel;
import dev.henny.r3.sync.SyncCodec;
import dev.henny.r3.sync.SyncMessage;
import dev.henny.r3.sync.SyncReceiver;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.net.VibeyNet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public final class JsonChannel implements SyncChannel {
    public static final String NAME = "video:screen.2";
    static final StreamCodec<FriendlyByteBuf, String> CODEC = StreamCodec.of(
            (buf, json) -> buf.writeUtf(json, SyncCodec.MAX_LENGTH),
            buf -> buf.readUtf(SyncCodec.MAX_LENGTH));

    private VibeyNet.Channel<String> channel;
    private SyncReceiver receiver;

    void register() {
        if (channel != null) return;
        channel = VibeyNet.channel(NAME, CODEC).onReceive(this::receive);
    }

    private void receive(UUID sender, String json) {
        if (receiver == null) return;
        try {
            receiver.receive(sender, SyncCodec.decode(json));
        } catch (IllegalArgumentException e) {
            Vibey.LOG.debug("Dropped a video screen message from {}: {}", sender, e.getMessage());
        }
    }

    @Override
    public boolean available() {
        return channel != null && VibeyNet.available();
    }

    @Override
    public void broadcast(SyncMessage message) {
        if (available()) channel.broadcast(SyncCodec.encode(message));
    }

    @Override
    public void sendTo(UUID player, SyncMessage message) {
        if (available()) channel.sendTo(player, SyncCodec.encode(message));
    }

    @Override
    public void receiver(SyncReceiver receiver) {
        this.receiver = receiver;
    }
}
