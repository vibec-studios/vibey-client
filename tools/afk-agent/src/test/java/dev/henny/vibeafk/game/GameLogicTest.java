package dev.henny.vibeafk.game;

import dev.henny.vibeafk.AfkConfig;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.henny.vibeafk.game.GameBridge.ScreenKind.*;
import static org.junit.jupiter.api.Assertions.*;

class GameLogicTest {
    static final String PROGUARD = String.join("\n",
            "# {\"fileName\":\"client.jar\"}",
            "net.minecraft.client.Minecraft -> fgo:",
            "    net.minecraft.client.player.LocalPlayer player -> t",
            "    1:5:net.minecraft.client.Minecraft getInstance() -> V",
            "    net.minecraft.client.multiplayer.ServerData getCurrentServer() -> Z",
            "net.minecraft.client.player.LocalPlayer -> gkx:",
            "    10:12:void chat(java.lang.String) -> a",
            "    void chat(java.lang.String,net.minecraft.network.chat.Component) -> a",
            "    int[] weird(float[][]) -> b",
            "net.minecraft.network.chat.Component -> xq:",
            "net.minecraft.client.multiplayer.ServerData -> fwx:",
            "    java.lang.String ip -> b",
            "net.minecraft.world.level.Level -> dcw:",
            "    void tick() -> m",
            "");

    static final String TINY = String.join("\n",
            "v1\tofficial\tintermediary",
            "CLASS\tfgo\tnet/minecraft/class_310",
            "CLASS\tgkx\tnet/minecraft/class_746",
            "CLASS\txq\tnet/minecraft/class_2561",
            "CLASS\tfwx\tnet/minecraft/class_642",
            "FIELD\tfgo\tLgkx;\tt\tfield_1724",
            "METHOD\tfgo\t()Lfgo;\tV\tmethod_1551",
            "METHOD\tgkx\t(Ljava/lang/String;)V\ta\tmethod_3142",
            "METHOD\tgkx\t(Ljava/lang/String;Lxq;)V\ta\tmethod_44096",
            "FIELD\tfwx\tLjava/lang/String;\tb\tfield_3761",
            "");

    @Test
    void proguardOnly() throws Exception {
        ProGuard pg = ProGuard.parse(new StringReader(PROGUARD), GameBridge.MEMBER_CLASSES);
        MappedNames n = new MappedNames(pg, null);
        assertEquals("obf", n.kind());
        assertEquals("fgo", n.cls(GameBridge.MINECRAFT));
        assertEquals(GameBridge.MINECRAFT, n.mojOf("fgo"));
        assertEquals("t", n.field(GameBridge.MINECRAFT, "player"));
        List<Names.Member> chat = n.methods(GameBridge.LOCAL_PLAYER, "chat");
        assertEquals(2, chat.size());
        assertArrayEquals(new String[]{"java.lang.String"}, chat.get(0).runtimeParams);
        assertArrayEquals(new String[]{"java.lang.String", "xq"}, chat.get(1).runtimeParams);
        assertEquals("[[F", n.runtimeTypeName("float[][]"));
        assertEquals("[Lxq;", n.runtimeTypeName("net.minecraft.network.chat.Component[]"));
        assertEquals("(Ljava/lang/String;Lxq;)V".substring(0, 0) + "Lxq;", n.officialDesc("net.minecraft.network.chat.Component"));
        assertNull(pg.methods.get("net.minecraft.world.level.Level"), "members of other classes are not kept");
        assertEquals("dcw", pg.mojToObf.get("net.minecraft.world.level.Level"));
    }

    @Test
    void proguardPlusIntermediary() throws Exception {
        ProGuard pg = ProGuard.parse(new StringReader(PROGUARD), GameBridge.MEMBER_CLASSES);
        Set<String> owners = new HashSet<>();
        for (String m : GameBridge.MEMBER_CLASSES) if (pg.mojToObf.containsKey(m)) owners.add(pg.mojToObf.get(m));
        Tiny t = Tiny.parse(new StringReader(TINY), "official", "intermediary", owners);
        MappedNames n = new MappedNames(pg, t);
        assertEquals("intermediary", n.kind());
        assertEquals("net.minecraft.class_310", n.cls(GameBridge.MINECRAFT));
        assertEquals(GameBridge.MINECRAFT, n.mojOf("net.minecraft.class_310"));
        assertEquals("field_1724", n.field(GameBridge.MINECRAFT, "player"));
        assertEquals("field_3761", n.field(GameBridge.SERVER_DATA, "ip"));
        assertEquals("method_1551", n.methods(GameBridge.MINECRAFT, "getInstance").get(0).runtimeName);
        List<Names.Member> chat = n.methods(GameBridge.LOCAL_PLAYER, "chat");
        assertEquals("method_3142", chat.get(0).runtimeName);
        assertEquals("method_44096", chat.get(1).runtimeName);
        assertArrayEquals(new String[]{"java.lang.String", "net.minecraft.class_2561"}, chat.get(1).runtimeParams);
        assertEquals("Z", n.methods(GameBridge.MINECRAFT, "getCurrentServer").get(0).runtimeName, "unknown in tiny keeps the official name");
    }

    @Test
    void tinyV2Header() throws Exception {
        String v2 = "tiny\t2\t0\tofficial\tintermediary\nc\tfgo\tnet/minecraft/class_310\n\tm\t()Lfgo;\tV\tmethod_1551\n";
        Tiny t = Tiny.parse(new StringReader(v2), "official", "intermediary", Set.of("fgo"));
        assertEquals("net/minecraft/class_310", t.classes.get("fgo"));
        assertEquals("method_1551", t.members.get(Tiny.key("fgo", "V", "()Lfgo;")));
    }

    @Test
    void backoffDoubles() {
        assertEquals(10, Backoff.delay(0, 10, 300));
        assertEquals(20, Backoff.delay(1, 10, 300));
        assertEquals(160, Backoff.delay(4, 10, 300));
        assertEquals(300, Backoff.delay(5, 10, 300));
        assertEquals(300, Backoff.delay(40, 10, 300));
    }

    private static AfkConfig cfg(boolean reconnect, int attempts) {
        return new AfkConfig("s", true, 2, true, true, reconnect, attempts, 10, 300, 6, 6, null);
    }

    private static GameWatcher.Sample s(GameBridge.ScreenKind k, Float h, Integer food, int screen) {
        return new GameWatcher.Sample(k, k == WORLD || k == DEATH ? "play.example.net" : null, h, 20f, food,
                k == DEATH ? "Alex was slain by Zombie" : null, k == DISCONNECTED ? "Kicked for being AFK" : null, screen);
    }

    @Test
    void watcherEventsAndReconnect() {
        List<String> ev = new ArrayList<>();
        List<Map<String, Object>> fields = new ArrayList<>();
        GameWatcher w = new GameWatcher((type, f) -> {
            ev.add(type);
            fields.add(f);
        });
        AfkConfig c = cfg(true, 2);
        w.sample(s(MENU, null, null, 1), c, 0);
        assertTrue(ev.isEmpty());
        w.sample(s(WORLD, 20f, 20, 2), c, 1000);
        assertEquals(List.of("connect", "health"), ev);
        w.sample(s(WORLD, 5f, 5, 2), c, 2000);
        assertEquals(List.of("connect", "health", "health_low", "food_low"), ev);
        w.sample(s(WORLD, 6f, 5, 2), c, 3000);
        w.sample(s(WORLD, 4f, 5, 2), c, 4000);
        assertEquals(4, ev.size(), "no repeat until recovered above threshold + 2");
        w.sample(s(DEATH, 0f, 5, 3), c, 5000);
        assertEquals("death", ev.get(4));
        assertEquals("Alex was slain by Zombie", fields.get(4).get("message"));
        w.sample(s(WORLD, 20f, 20, 2), c, 6000);
        assertEquals("respawn", ev.get(5));
        ev.clear();
        w.sample(s(DISCONNECTED, null, null, 7), c, 10_000);
        assertEquals(List.of("disconnect"), ev);
        assertEquals("Kicked for being AFK", fields.get(fields.size() - 1).get("reason"));
        assertEquals("play.example.net", fields.get(fields.size() - 1).get("server"));
        w.sample(s(DISCONNECTED, null, null, 7), c, 15_000);
        assertFalse(w.takeReconnect());
        w.sample(s(DISCONNECTED, null, null, 7), c, 20_000);
        assertTrue(w.takeReconnect());
        assertEquals("reconnect_attempt", ev.get(1));
        assertEquals(1, fields.get(fields.size() - 1).get("attempt"));
        w.sample(s(CONNECTING, null, null, 8), c, 21_000);
        w.sample(s(DISCONNECTED, null, null, 9), c, 22_000);
        assertEquals("disconnect", ev.get(2));
        w.sample(s(DISCONNECTED, null, null, 9), c, 42_000);
        assertTrue(w.takeReconnect());
        assertEquals(2, fields.get(fields.size() - 1).get("attempt"));
        w.sample(s(DISCONNECTED, null, null, 10), c, 43_000);
        assertEquals("reconnect_gave_up", ev.get(ev.size() - 1));
        w.sample(s(DISCONNECTED, null, null, 10), c, 500_000);
        assertFalse(w.takeReconnect());
    }

    @Test
    void watcherLeavingAndNoReconnectWhenOff() {
        List<String> ev = new ArrayList<>();
        List<Map<String, Object>> fields = new ArrayList<>();
        GameWatcher w = new GameWatcher((type, f) -> {
            ev.add(type);
            fields.add(f);
        });
        AfkConfig off = cfg(false, 5);
        w.sample(s(WORLD, 20f, 20, 1), off, 0);
        w.sample(s(MENU, null, null, 2), off, 1000);
        assertEquals("disconnect", ev.get(ev.size() - 1));
        assertEquals(Boolean.TRUE, fields.get(fields.size() - 1).get("left"));
        w.sample(s(WORLD, 20f, 20, 3), off, 2000);
        w.sample(s(DISCONNECTED, null, null, 4), off, 3000);
        w.sample(s(DISCONNECTED, null, null, 4), off, 300_000);
        assertFalse(w.takeReconnect());
    }
}
