package dev.henny.vibeyclient.module.companion;

import dev.henny.companio.mc.CompanioMc;
import dev.henny.companio.sync.SnapshotCodec;
import dev.henny.vibeyclient.api.net.VibeyNet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

final class CompanionSync {
    static final String CHANNEL = "companion:state";
    private static final StreamCodec<FriendlyByteBuf, String> CODEC = StreamCodec.of(
            (buf, s) -> buf.writeUtf(s, SnapshotCodec.MAX_BYTES), buf -> buf.readUtf(SnapshotCodec.MAX_BYTES));

    private VibeyNet.Channel<String> channel;
    private boolean connected;

    void load() {
        channel = VibeyNet.channel(CHANNEL, CODEC);
        channel.onReceive(CompanioMc::receive);
    }

    void tick(boolean share) {
        boolean want = share && VibeyNet.available();
        if (want == connected) return;
        connected = want;
        CompanioMc.sync(want ? payload -> channel.broadcast(payload) : null);
    }

    void leave() {
        connected = false;
        CompanioMc.sync(null);
    }
}
