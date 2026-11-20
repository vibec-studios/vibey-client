package dev.henny.vibeyclient.module.social.discord.core;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RpcPayloadTest {
    private static RpcInputs in(RpcPlace place, String status, boolean shown, boolean allowed, boolean streamer) {
        return new RpcInputs(shown, status, place, "play.example.net", allowed, streamer, 1_700_000_000_123L, "Vibey Client 0.3.1 · 1.21.11");
    }

    @Test
    void multiplayerHidesTheServerByDefault() {
        JsonObject a = RpcPayload.build(in(RpcPlace.MULTIPLAYER, "online", true, false, false), RpcLabels.EN);
        assertEquals("Multiplayer", a.get("details").getAsString());
        assertFalse(a.has("state"));
        assertFalse(a.toString().contains("example"));
        assertEquals(1_700_000_000L, a.getAsJsonObject("timestamps").get("start").getAsLong());
        assertEquals("vibey", a.getAsJsonObject("assets").get("large_image").getAsString());
    }

    @Test
    void serverOnlyWhenAllowedAndNotStreaming() {
        assertEquals("play.example.net", RpcPayload.build(in(RpcPlace.MULTIPLAYER, "online", true, true, false), RpcLabels.EN)
                .get("state").getAsString());
        assertFalse(RpcPayload.build(in(RpcPlace.MULTIPLAYER, "online", true, true, true), RpcLabels.EN).toString().contains("example"));
        assertFalse(RpcPayload.showsServer(in(RpcPlace.SINGLEPLAYER, "online", true, true, false)));
    }

    @Test
    void privacyClearsTheActivity() {
        assertNull(RpcPayload.build(in(RpcPlace.SINGLEPLAYER, "online", false, true, false), RpcLabels.EN));
        assertNull(RpcPayload.build(in(RpcPlace.MENU, "invisible", true, true, false), RpcLabels.EN));
    }

    @Test
    void statusBecomesTheState() {
        assertEquals("Away", RpcPayload.build(in(RpcPlace.SINGLEPLAYER, "away", true, false, false), RpcLabels.EN).get("state").getAsString());
        assertEquals("Hosting a world", RpcPayload.build(in(RpcPlace.HOSTING, "online", true, false, false), RpcLabels.EN).get("details").getAsString());
    }

    @Test
    void framesRoundTripLittleEndian() throws IOException {
        IpcFrame f = new IpcFrame(IpcOp.FRAME, "{\"a\":\"ä\"}");
        byte[] bytes = f.encode();
        assertEquals(1, bytes[0]);
        assertEquals(10, bytes[4]);
        assertEquals(f, IpcFrame.read(new ByteArrayInputStream(bytes)));
        ByteArrayOutputStream big = new ByteArrayOutputStream();
        big.write(new byte[] {1, 0, 0, 0, 0, 0, 0, 0x7f});
        assertThrows(IOException.class, () -> IpcFrame.read(new ByteArrayInputStream(big.toByteArray())));
    }

    @Test
    void pipePaths() {
        assertTrue(PipePaths.windows("Windows 11"));
        assertEquals("\\\\.\\pipe\\discord-ipc-0", PipePaths.windowsPipes().get(0));
        List<String> unix = PipePaths.unixSockets(Map.of("XDG_RUNTIME_DIR", "/run/user/1000"));
        assertEquals("/run/user/1000/discord-ipc-0", unix.get(0));
        assertTrue(unix.contains("/tmp/discord-ipc-9"));
        assertTrue(unix.contains("/run/user/1000/app/com.discordapp.Discord/discord-ipc-0"));
    }
}
