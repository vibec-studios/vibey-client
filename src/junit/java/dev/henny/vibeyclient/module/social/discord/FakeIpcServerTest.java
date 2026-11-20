package dev.henny.vibeyclient.module.social.discord;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.module.social.discord.core.IpcClient;
import dev.henny.vibeyclient.module.social.discord.core.IpcException;
import dev.henny.vibeyclient.module.social.discord.core.RpcInputs;
import dev.henny.vibeyclient.module.social.discord.core.RpcLabels;
import dev.henny.vibeyclient.module.social.discord.core.RpcPayload;
import dev.henny.vibeyclient.module.social.discord.core.RpcPlace;
import dev.henny.vibeyclient.module.social.discord.ipc.UnixSocketConnector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakeIpcServerTest {
    @TempDir
    Path dir;

    private static JsonObject activity() {
        return RpcPayload.build(new RpcInputs(true, "online", RpcPlace.SINGLEPLAYER, null, false, false, 1000L, "v"), RpcLabels.EN);
    }

    @Test
    void handshakeSetAndClear() throws Exception {
        try (FakeDiscord discord = new FakeDiscord(dir); IpcClient client = new IpcClient(new UnixSocketConnector(List.of(discord.socket.toString())), 42)) {
            discord.pingFirst = true;
            JsonObject ready = client.connect("123456");
            assertEquals("READY", ready.get("evt").getAsString());
            assertEquals(List.of("123456"), discord.handshakes);
            client.setActivity(activity());
            client.setActivity(null);
            assertEquals(2, discord.commands.size());
            JsonObject first = discord.commands.get(0);
            assertEquals("SET_ACTIVITY", first.get("cmd").getAsString());
            assertEquals(42, first.getAsJsonObject("args").get("pid").getAsLong());
            assertEquals("Singleplayer", first.getAsJsonObject("args").getAsJsonObject("activity").get("details").getAsString());
            assertTrue(discord.commands.get(1).getAsJsonObject("args").get("activity").isJsonNull());
        }
    }

    @Test
    void errorsAreReported() throws Exception {
        try (FakeDiscord discord = new FakeDiscord(dir); IpcClient client = new IpcClient(new UnixSocketConnector(List.of(discord.socket.toString())), 1)) {
            discord.errorFor = "1";
            client.connect("1");
            IpcException e = assertThrows(IpcException.class, () -> client.setActivity(activity()));
            assertTrue(e.getMessage().contains("nope"));
        }
    }

    @Test
    void notRunningFailsQuietly() {
        IpcClient client = new IpcClient(new UnixSocketConnector(List.of(dir.resolve("missing").toString())), 1);
        assertThrows(java.io.IOException.class, () -> client.connect("1"));
        assertFalse(client.connected());
    }

    @Test
    void workerPushesAndStops() throws Exception {
        try (FakeDiscord discord = new FakeDiscord(dir)) {
            DiscordWorker w = new DiscordWorker(new UnixSocketConnector(List.of(discord.socket.toString())), "777", s -> {});
            w.push(activity());
            assertTrue(discord.awaitCommand());
            assertTrue(w.connected());
            assertEquals(List.of("777"), discord.handshakes);
            w.stop();
        }
    }
}
