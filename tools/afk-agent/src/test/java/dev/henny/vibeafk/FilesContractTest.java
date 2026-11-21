package dev.henny.vibeafk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FilesContractTest {
    @TempDir
    Path tmp;

    @Test
    void jsonRoundTrip() {
        Map<String, Object> o = Json.parseObject("{\"a\":1,\"b\":[true,null,\"x\\n\\u00e4\"],\"c\":{\"d\":-2.5e1}}");
        assertEquals(1.0, o.get("a"));
        assertEquals("{\"a\":1.0,\"b\":[true,null,\"x\\n\u00e4\"],\"c\":{\"d\":-25.0}}", Json.write(o));
        assertEquals("{\"n\":3,\"s\":\"q\\\"\\\\\\u0001\"}", Json.write(Json.obj("n", 3, "s", "q\"\\\u0001")));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\":}"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("[1] x"));
    }

    @Test
    void configDefaultsAndClamping() {
        AfkConfig c = AfkConfig.parse("{\"schema\":1,\"mode\":\"background\",\"background_fps\":99,\"future\":{\"x\":1},"
                + "\"auto_reconnect\":{\"enabled\":true,\"base_delay_s\":1,\"max_delay_s\":2},\"alerts\":{\"low_health\":4.5},"
                + "\"mappings\":\"C:/m.txt\",\"session\":\"abc\"}");
        assertTrue(c.background);
        assertEquals(10, c.backgroundFps);
        assertTrue(c.mute);
        assertTrue(c.hideWindow);
        assertTrue(c.autoReconnect);
        assertEquals(5, c.baseDelayS);
        assertEquals(10, c.maxDelayS);
        assertEquals(5, c.maxAttempts);
        assertEquals(4.5, c.lowHealth);
        assertEquals(6, c.lowFood);
        assertEquals("C:/m.txt", c.mappings);
        assertEquals("abc", c.session);
        AfkConfig f = AfkConfig.parse("{}");
        assertFalse(f.background);
        assertEquals(2, f.backgroundFps);
        assertFalse(f.autoReconnect);
    }

    @Test
    void outboxItems() throws Exception {
        OutboxItem c = OutboxItem.parse("{\"id\":\"b1\",\"type\":\"command\",\"text\":\"/spawn \"}", 5);
        assertTrue(c.command);
        assertEquals("spawn", c.text);
        OutboxItem m = OutboxItem.parse("{\"id\":\"b2\",\"type\":\"chat\",\"text\":\"hallo \u00e4\"}", 5);
        assertFalse(m.command);
        OutboxItem.Invalid e = assertThrows(OutboxItem.Invalid.class,
                () -> OutboxItem.parse("{\"id\":\"b3\",\"type\":\"chat\",\"text\":\"a\\nb\"}", 0));
        assertEquals("b3", e.id);
        assertThrows(OutboxItem.Invalid.class, () -> OutboxItem.parse("{\"id\":\"b4\",\"type\":\"chat\",\"text\":\"\u00a7cRot\"}", 0));
        assertThrows(OutboxItem.Invalid.class, () -> OutboxItem.parse("{\"id\":\"b5\",\"type\":\"chat\",\"text\":\"" + "x".repeat(257) + "\"}", 0));
        assertNull(assertThrows(OutboxItem.Invalid.class, () -> OutboxItem.parse("{\"id\":\"bad id\",\"type\":\"chat\",\"text\":\"x\"}", 0)).id);
        assertThrows(OutboxItem.Invalid.class, () -> OutboxItem.parse("nope", 0));
    }

    @Test
    void rateLimiterBurstThenInterval() {
        RateLimiter r = new RateLimiter(3, 1500, 0);
        assertTrue(r.tryAcquire(0));
        assertTrue(r.tryAcquire(0));
        assertTrue(r.tryAcquire(0));
        assertFalse(r.tryAcquire(100));
        assertFalse(r.tryAcquire(1400));
        assertTrue(r.tryAcquire(1500));
        assertFalse(r.tryAcquire(1600));
        assertTrue(r.tryAcquire(30_000));
        assertTrue(r.tryAcquire(30_000));
        assertTrue(r.tryAcquire(30_000));
        assertFalse(r.tryAcquire(30_000));
    }

    @Test
    void eventLogSequenceAndRotation() throws Exception {
        File dir = tmp.toFile();
        long[] now = {1000};
        EventLog log = new EventLog(dir, 300, () -> now[0]);
        for (int k = 0; k < 10; k++) {
            now[0]++;
            log.emit("chat", Json.obj("text", "message number " + k, "seq", 999));
        }
        assertEquals(10, log.seq());
        File cur = new File(dir, EventLog.FILE);
        File old = new File(dir, EventLog.ROTATED);
        assertTrue(old.isFile());
        assertTrue(cur.length() <= 300);
        List<String> lines = new ArrayList<>(Files.readAllLines(old.toPath()));
        lines.addAll(Files.readAllLines(cur.toPath()));
        long last = Long.parseLong(lines.get(lines.size() - 1).replaceAll("^\\{\"seq\":(\\d+),.*", "$1"));
        assertEquals(10, last);
        Map<String, Object> first = Json.parseObject(Files.readAllLines(cur.toPath()).get(0));
        assertEquals("chat", first.get("type"));
        assertTrue(((Double) first.get("seq")) > 1);
    }

    @Test
    void outboxReaderKeepsPartialLinesAndHandlesTruncation() throws Exception {
        File dir = tmp.toFile();
        File f = new File(dir, OutboxReader.FILE);
        OutboxReader r = new OutboxReader(dir);
        List<String> got = new ArrayList<>();
        OutboxReader.Sink sink = new OutboxReader.Sink() {
            @Override
            public void line(String line) {
                got.add(line);
            }

            @Override
            public void tooLarge() {
                got.add("TOO_LARGE");
            }
        };
        append(f, "{\"id\":\"1\"}\n{\"id\":\"2\"");
        r.poll(sink);
        assertEquals(List.of("{\"id\":\"1\"}"), got);
        append(f, "}\n");
        r.poll(sink);
        assertEquals(List.of("{\"id\":\"1\"}", "{\"id\":\"2\"}"), got);
        Files.write(f.toPath(), new byte[0]);
        r.poll(sink);
        append(f, "{\"id\":\"3\"}\n");
        r.poll(sink);
        assertEquals("{\"id\":\"3\"}", got.get(2));
    }

    @Test
    void logTailSkipsOldFileAndFollowsReplacement() throws Exception {
        File logs = tmp.resolve("logs").toFile();
        logs.mkdirs();
        File f = new File(logs, "latest.log");
        Files.write(f.toPath(), "[10:00:00] [main/INFO]: old session\n[10:00:01] [Render thread/INFO]: [CHAT] old chat\n".getBytes(StandardCharsets.UTF_8));
        LogTail t = new LogTail(f);
        t.skipExisting();
        List<String> got = new ArrayList<>();
        t.poll(got::add);
        assertTrue(got.isEmpty());
        String fresh = "[11:22:33] [main/INFO]: Setting user: Alex\n[11:22:40] [Render thread/INFO]: [System] [CHAT] <Steve> hi Alex, ich bin's und das hier ist eine laengere Zeile damit die Datei groesser wird\n[11:22:41] [Render";
        Files.write(f.toPath(), fresh.getBytes(StandardCharsets.UTF_8));
        t.poll(got::add);
        assertEquals(2, got.size());
        append(f, " thread/INFO]: [CHAT] zweite\n");
        t.poll(got::add);
        assertEquals("[11:22:41] [Render thread/INFO]: [CHAT] zweite", got.get(2));
    }

    @Test
    void chatClassification() {
        ChatParser.Line l = ChatParser.parseLogLine("[11:22:40] [Render thread/INFO]: [System] [CHAT] <Steve> hey \u00a7calex\u00a7r, komm mal");
        assertNotNull(l);
        assertEquals("<Steve> hey alex, komm mal", l.text);
        assertEquals("Steve", l.sender);
        assertFalse(l.whisper);
        assertTrue(l.mentions("Alex"));
        assertFalse(l.mentions("Ale"));
        assertFalse(ChatParser.classify("<Alex> ich bin Alex").mentions("Alex"));
        ChatParser.Line w = ChatParser.classify("Steve whispers to you: psst");
        assertTrue(w.whisper);
        assertEquals("Steve", w.sender);
        assertTrue(ChatParser.classify("[Steve -> me] psst").whisper);
        assertTrue(ChatParser.classify("Steve fl\u00fcstert dir zu: psst").whisper);
        assertEquals("Notch", ChatParser.classify("[Admin] Notch \u00bb hallo").sender);
        assertNull(ChatParser.classify("Alex joined the game").sender);
        assertNull(ChatParser.parseLogLine("[11:00:00] [Render thread/INFO]: Loaded 12 recipes"));
        assertEquals("Alex", ChatParser.userFromLog("[11:22:33] [main/INFO]: Setting user: Alex"));
    }

    @Test
    void vibeDirArgument() {
        assertEquals(new File("C:/Spiele/Meine Instanz, 2/.vibe"), Agent.vibeDir("C:/Spiele/Meine Instanz, 2/.vibe"));
        assertEquals(new File("C:/x/.vibe"), Agent.vibeDir("dir=C:/x/.vibe"));
        assertEquals(new File(System.getProperty("user.dir"), ".vibe"), Agent.vibeDir(""));
    }

    @Test
    void loaderDetection() {
        assertEquals("fabric", Env.loader("net.fabricmc.loader.impl.launch.knot.KnotClient --version x", ""));
        assertEquals("quilt", Env.loader("org.quiltmc.loader.impl.launch.knot.KnotClient", ""));
        assertEquals("neoforge", Env.loader("cpw.mods.bootstraplauncher.BootstrapLauncher --launchTarget forgeclient",
                "C:/libs/net/neoforged/fancymodloader/loader.jar"));
        assertEquals("forge", Env.loader("cpw.mods.bootstraplauncher.BootstrapLauncher --launchTarget forgeclient",
                "C:/libs/net/minecraftforge/fmlloader.jar"));
        assertEquals("vanilla", Env.loader("net.minecraft.client.main.Main --username A", "C:/v/1.21.11.jar"));
        assertEquals("1.21.11", Env.argument("net.minecraft.client.main.Main --version 1.21.11 --x", "--version"));
    }

    private static void append(File f, String s) throws Exception {
        try (FileOutputStream out = new FileOutputStream(f, true)) {
            out.write(s.getBytes(StandardCharsets.UTF_8));
        }
    }
}
