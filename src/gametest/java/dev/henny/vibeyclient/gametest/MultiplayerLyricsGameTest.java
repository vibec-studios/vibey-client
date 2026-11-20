package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.service.HostingService;
import dev.henny.vibeyclient.module.media.lyrics.LyricsConfig;
import dev.henny.vibeyclient.module.media.lyrics.LyricsFeature;
import dev.henny.vibeyclient.module.media.lyrics.Playback;
import dev.henny.vibeyclient.module.media.lyrics.library.Song;
import dev.henny.vibeyclient.module.media.lyrics.library.SongLibrary;
import dev.henny.vibeyclient.module.media.lyrics.world.WorldText;
import dev.henny.vibeyclient.module.media.lyrics.screen.AdvancedScreen;
import dev.henny.vibeyclient.module.media.lyrics.screen.LyricsScreen;
import dev.henny.vibeyclient.module.media.lyrics.screen.SearchScreen;
import dev.henny.vibeyclient.module.multiplayer.MultiplayerModule;
import dev.henny.vibeyclient.module.multiplayer.host.HostSession;
import dev.henny.vibeyclient.module.multiplayer.screen.AdminScreen;
import dev.henny.vibeyclient.module.multiplayer.screen.HostScreen;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import dev.henny.vibeyclient.api.ModuleManager.Status;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The multiplayer and lyrics modules in the real game: their settings pages, the pause menu entry, hosting a world
 * through a LOCAL relay (start it first: {@code cd vibey-multiplayer/relay && ALLOW_OFFLINE=1 PORT=26400 node
 * src/main.ts}; address via {@code -Dvibeyclient.test.relay}, default 127.0.0.1:26400; skipped with
 * {@code -Dvibeyclient.test.relay=off}), a Minecraft status ping through the relay into the hosted world, and the
 * demo song in the sky, in front of you and on the HUD. Screenshots: versions/fabric-1.21.11/build/run/clientGameTest/screenshots.
 */
public final class MultiplayerLyricsGameTest implements FabricClientGameTest {
    private static final String RELAY = System.getProperty("vibeyclient.test.relay", "127.0.0.1:26400");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 720);
        if (FabricLoader.getInstance().isModLoaded("vibey") || FabricLoader.getInstance().isModLoaded("vibeylyrics")) {
            conflict(context);
            return;
        }
        List<HostingService.HostingChanged> events = new CopyOnWriteArrayList<>();
        context.runOnClient(mc -> {
            ModuleManager mm = ModuleManager.get();
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            if (!mm.isEnabled("multiplayer")) throw new AssertionError("multiplayer should be on: " + mm.get("multiplayer").status());
            if (!mm.isEnabled("media")) throw new AssertionError("media should be on: " + mm.get("media").status());
            if (!LyricsFeature.running()) throw new AssertionError("lyrics part of media should run: " + dev.henny.vibeyclient.module.media.MediaModule.instance().describe());
            HostingService hs = Services.get(HostingService.class).orElseThrow(() -> new AssertionError("HostingService missing"));
            if (hs.canHost()) throw new AssertionError("canHost on the title screen");
            Events.subscribe(HostingService.HostingChanged.class, events::add);
            // runtime toggle: the service follows the module
            mm.setEnabled("multiplayer", false);
            if (Services.get(HostingService.class).isPresent()) throw new AssertionError("HostingService still there while off");
            mm.setEnabled("multiplayer", true);
            if (Services.get(HostingService.class).isEmpty()) throw new AssertionError("HostingService not back after enabling");
            System.out.println("[vibeyclient-mp-test] modules ok, HostingService follows the module");
        });

        ModulesPanel.preselect("multiplayer");
        context.setScreen(() -> new VibeyScreen(null));
        context.waitTicks(10);
        context.takeScreenshot("mp_1_settings");
        ModulesPanel.preselect("media");
        context.setScreen(() -> new VibeyScreen(null));
        context.waitTicks(10);
        context.takeScreenshot("ly_1_settings");
        context.setScreen(() -> null);

        try (TestSingleplayerContext world = context.worldBuilder()
                .setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeylyrics"))
                .create()) {
            world.getServer().runCommand("time set 12000");
            world.getServer().runCommand("weather clear");
            world.getClientWorld().waitForChunksRender();

            multiplayer(context, events);
            lyrics(context, world);
        }
        context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
        System.out.println("[vibeyclient-mp-test] done");
    }

    // ============================================================================================ conflicts

    /**
     * Run with the standalone Vibey Multiplayer / Vibey Lyrics jars in the mods folder: the matching modules must be
     * off with a reason, register nothing (no service, no keybinds, no pause buttons) and their mixins must not apply.
     */
    private static void conflict(ClientGameTestContext context) {
        boolean mp = FabricLoader.getInstance().isModLoaded("vibey");
        boolean ly = FabricLoader.getInstance().isModLoaded("vibeylyrics");
        context.runOnClient(mc -> {
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            ModuleManager mm = ModuleManager.get();
            for (String id : new String[]{"multiplayer"}) {
                boolean expect = mp;
                var m = mm.get(id);
                System.out.println("[vibeyclient-conflict-test] " + id + ": " + m.status() + " (" + m.statusReason() + ")");
                if (expect && (m.status() != Status.CONFLICT || m.isEnabled())) throw new AssertionError(id + " should be in conflict, is " + m.status());
                if (expect && (mm.setEnabled(id, true) || m.isEnabled())) throw new AssertionError(id + " could be switched on");
            }
            if (mp && (Services.get(HostingService.class).isPresent() || MultiplayerModule.instance() != null)) {
                throw new AssertionError("multiplayer module loaded although Vibey Multiplayer is installed");
            }
            // Vibey Lyrics replaces only the lyrics part: Media itself stays on, the lyrics part is off with a reason.
            var media = dev.henny.vibeyclient.module.media.MediaModule.instance();
            System.out.println("[vibeyclient-conflict-test] media: " + mm.get("media").status() + " parts: " + media.describe() + " notices: " + media.notices());
            if (ly && !mm.isEnabled("media")) throw new AssertionError("media should stay on with Vibey Lyrics installed");
            if (ly && (LyricsFeature.instance().usable() || LyricsFeature.running())) throw new AssertionError("lyrics part runs although Vibey Lyrics is installed");
            if (ly && media.notices().isEmpty()) throw new AssertionError("no notice about the lyrics conflict");
            for (var k : mc.options.keyMappings) {
                String name = k.getName();
                if ((mp && name.startsWith("key.vibeyclient.multiplayer.")) || (ly && name.startsWith("key.vibeyclient.media.lyrics."))) {
                    throw new AssertionError("Keybind of a module in conflict registered: " + name);
                }
            }
            // Our mixin handlers carry "vibeyclient$lyrics$" in their names; the standalone's "vibeylyrics$".
            if (ly) {
                int ours = 0;
                int theirs = 0;
                for (Class<?> c : new Class<?>[]{net.minecraft.client.renderer.SkyRenderer.class, net.minecraft.client.renderer.CloudRenderer.class,
                        net.minecraft.client.renderer.fog.FogRenderer.class, net.minecraft.client.renderer.LevelRenderer.class}) {
                    for (var method : c.getDeclaredMethods()) {
                        if (method.getName().contains("vibeyclient$lyrics$")) ours++;
                        if (method.getName().contains("vibeylyrics$")) theirs++;
                    }
                }
                System.out.println("[vibeyclient-conflict-test] mixin handlers: ours " + ours + ", standalone " + theirs);
                if (ours != 0) throw new AssertionError("Lyrics module mixins applied although Vibey Lyrics is installed");
            }
        });
        ModulesPanel.preselect("multiplayer");
        context.setScreen(() -> new VibeyScreen(null));
        context.waitTicks(10);
        context.takeScreenshot("conflict_1_menu_multiplayer");
        ModulesPanel.preselect("media");
        context.setScreen(() -> new VibeyScreen(null));
        context.waitTicks(10);
        context.takeScreenshot("conflict_2_menu_media");
        context.setScreen(() -> null);
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            context.setScreen(() -> new PauseScreen(true));
            context.waitTicks(5);
            List<String> labels = context.computeOnClient(mc -> mc.screen.children().stream()
                    .filter(c -> c instanceof Button).map(c -> ((Button) c).getMessage().getString()).toList());
            System.out.println("[vibeyclient-conflict-test] pause menu buttons: " + labels);
            context.takeScreenshot("conflict_3_pause_menu");
            for (String label : new String[]{"Host World", "Vibey"}) {
                long n = labels.stream().filter(label::equals).count();
                if (n > 1) throw new AssertionError("Duplicate pause button " + label + ": " + labels);
            }
            if (labels.stream().filter(l -> l.contains("Lyrics")).count() > 1) throw new AssertionError("Duplicate lyrics button: " + labels);
            context.setScreen(() -> null);
        }
        context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
        System.out.println("[vibeyclient-conflict-test] done");
    }

    // ============================================================================================ multiplayer

    private static void multiplayer(ClientGameTestContext context, List<HostingService.HostingChanged> events) {
        boolean canHost = context.computeOnClient(mc -> Services.get(HostingService.class).map(HostingService::canHost).orElse(false));
        if (!canHost) throw new AssertionError("canHost false in a singleplayer world");

        context.setScreen(() -> new PauseScreen(true));
        context.waitTicks(5);
        int hostButtons = context.computeOnClient(mc -> (int) mc.screen.children().stream()
                .filter(c -> c instanceof Button b && b.getMessage().getString().equals("Host World")).count());
        boolean lan = context.computeOnClient(mc -> mc.screen.children().stream()
                .anyMatch(c -> c instanceof Button b && b.getMessage().getString().equals(net.minecraft.client.resources.language.I18n.get("menu.shareToLan"))));
        context.takeScreenshot("mp_2_pause_menu");
        if (hostButtons != 1 || lan) throw new AssertionError("Pause menu: " + hostButtons + " host buttons, open to LAN still there: " + lan);

        context.setScreen(() -> new HostScreen(null));
        context.waitTicks(10);
        context.takeScreenshot("mp_3_host_offline");

        if ("off".equals(RELAY)) {
            System.out.println("[vibeyclient-mp-test] relay test skipped");
            context.setScreen(() -> null);
            return;
        }
        context.runOnClient(mc -> {
            MultiplayerModule.instance().relay.set(RELAY);
            Services.get(HostingService.class).orElseThrow().startHosting();
        });
        context.waitFor(mc -> HostSession.status() == HostSession.Status.ONLINE || HostSession.status() == HostSession.Status.ERROR, 600);
        String err = context.computeOnClient(mc -> HostSession.error());
        if (HostSession.status() != HostSession.Status.ONLINE) {
            context.takeScreenshot("mp_4_host_error");
            throw new AssertionError("Hosting did not come online through " + RELAY + ": " + err);
        }
        context.waitTicks(10);
        context.takeScreenshot("mp_4_host_online");
        String address = context.computeOnClient(mc -> Services.get(HostingService.class).orElseThrow().address());
        int port = context.computeOnClient(mc -> Services.get(HostingService.class).orElseThrow().port());
        boolean hosting = context.computeOnClient(mc -> Services.get(HostingService.class).orElseThrow().isHosting());
        if (address == null || !hosting) throw new AssertionError("HostingService: address " + address + ", hosting " + hosting);
        if (events.stream().noneMatch(e -> e.hosting() && address.equals(e.address()))) {
            throw new AssertionError("No HostingChanged(true) event: " + events);
        }
        System.out.println("[vibeyclient-mp-test] online at " + address + ":" + port + ", events " + events);

        // A Minecraft status ping to the relay with the world's address: must come back from the hosted world.
        String relayHost = RELAY.substring(0, RELAY.lastIndexOf(':'));
        int relayPort = Integer.parseInt(RELAY.substring(RELAY.lastIndexOf(':') + 1));
        String status;
        try {
            status = statusPing(relayHost, relayPort, address, port);
        } catch (IOException e) {
            throw new AssertionError("Status ping through the relay failed", e);
        }
        if (!status.contains("\"version\"")) throw new AssertionError("Unexpected status through the relay: " + status);
        System.out.println("[vibeyclient-mp-test] status through the relay: " + status.substring(0, Math.min(200, status.length())));

        context.setScreen(() -> new AdminScreen(new HostScreen(null)));
        context.waitTicks(10);
        context.takeScreenshot("mp_5_admin_players");

        context.setScreen(() -> new PauseScreen(true));
        context.waitTicks(5);
        context.takeScreenshot("mp_6_pause_hosting");

        context.runOnClient(mc -> Services.get(HostingService.class).orElseThrow().stopHosting());
        context.waitFor(mc -> HostSession.status() == HostSession.Status.OFF, 100);
        context.waitTicks(2);
        if (events.isEmpty() || events.get(events.size() - 1).hosting()) throw new AssertionError("No HostingChanged(false) event: " + events);
        context.setScreen(() -> new HostScreen(null));
        context.waitTicks(10);
        context.takeScreenshot("mp_7_host_stopped");
        context.setScreen(() -> null);
        System.out.println("[vibeyclient-mp-test] stopped hosting, events " + events);
    }

    /** Handshake (next state: status) + status request, returns the status JSON. */
    private static String statusPing(String relayHost, int relayPort, String address, int port) throws IOException {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(relayHost, relayPort), 5000);
            s.setSoTimeout(10_000);
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            varInt(body, 0x00);
            varInt(body, 774);
            byte[] host = address.getBytes(StandardCharsets.UTF_8);
            varInt(body, host.length);
            body.write(host);
            body.write((port >> 8) & 0xFF);
            body.write(port & 0xFF);
            varInt(body, 1);
            OutputStream out = s.getOutputStream();
            ByteArrayOutputStream packet = new ByteArrayOutputStream();
            varInt(packet, body.size());
            body.writeTo(packet);
            varInt(packet, 1);
            packet.write(0x00);
            out.write(packet.toByteArray());
            out.flush();
            DataInputStream in = new DataInputStream(s.getInputStream());
            readVarInt(in);
            int id = readVarInt(in);
            if (id != 0) throw new IOException("Packet id " + id);
            byte[] json = new byte[readVarInt(in)];
            in.readFully(json);
            return new String(json, StandardCharsets.UTF_8);
        }
    }

    private static void varInt(ByteArrayOutputStream out, int v) {
        while ((v & ~0x7F) != 0) {
            out.write((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        out.write(v);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int v = 0;
        for (int i = 0; i < 5; i++) {
            int b = in.readUnsignedByte();
            v |= (b & 0x7F) << (7 * i);
            if ((b & 0x80) == 0) return v;
        }
        throw new IOException("VarInt too long");
    }

    // ============================================================================================ lyrics

    private static void lyrics(ClientGameTestContext context, TestSingleplayerContext world) {
        context.runOnClient(mc -> {
            SongLibrary.rescan();
            demo();
        });
        // Stand on a small glass platform for a clear view.
        int[] p = context.computeOnClient(mc -> new int[]{
                (int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ())});
        int top = p[1] + 14;
        world.getServer().runCommand(String.format("fill %d %d %d %d %d %d minecraft:glass", p[0] - 1, top, p[2] - 1, p[0] + 1, top, p[2] + 1));
        world.getServer().runCommand(String.format("tp @a %d.5 %d %d.5", p[0], top + 1, p[2]));
        context.waitTicks(80);

        run(context, mc -> {
            hideHud(mc, true);
            mc.player.setYRot(-45);
            mc.player.setXRot(-14);
            LyricsConfig c = LyricsConfig.get();
            c.position = LyricsConfig.Position.SKY;
            c.applyPreset(LyricsConfig.Preset.NEON);
            c.reveal = LyricsConfig.Reveal.LINE;
            c.skyMode = LyricsConfig.SkyMode.FIXED;
            c.skyYaw = -45;
            c.sky = "vanilla";
            Playback.play(demo());
        });
        scene(context, 25_350, "ly_2_sky_neon");
        long drawn = context.computeOnClient(mc -> WorldText.drawnFrames());
        if (drawn <= 0) throw new AssertionError("World lyrics were never drawn");
        System.out.println("[vibeyclient-ly-test] world hook drew " + drawn + " frames");

        run(context, mc -> {
            LyricsConfig c = LyricsConfig.get();
            c.applyPreset(LyricsConfig.Preset.LAVENDER);
            c.sky = "galaxy";
        });
        context.waitTicks(10);
        scene(context, 16_700, "ly_3_flight_galaxy_sky");

        run(context, mc -> {
            LyricsConfig c = LyricsConfig.get();
            c.sky = "vanilla";
            c.position = LyricsConfig.Position.FRONT;
            c.applyPreset(LyricsConfig.Preset.OCEAN);
            mc.player.setXRot(4);
        });
        scene(context, 30_700, "ly_4_front_ocean");

        run(context, mc -> {
            hideHud(mc, false);
            mc.player.setXRot(-14);
            LyricsConfig c = LyricsConfig.get();
            c.position = LyricsConfig.Position.HUD;
            c.applyPreset(LyricsConfig.Preset.MINIMAL);
        });
        scene(context, 11_000, "ly_5_hud_minimal");

        // Module off: no sky, no lyrics, no music; on again: back.
        run(context, mc -> {
            LyricsConfig.get().sky = "galaxy";
            LyricsConfig.get().position = LyricsConfig.Position.SKY;
            ModuleManager.get().setEnabled("media", false);
            if (LyricsFeature.running() || Playback.isPlaying()) throw new AssertionError("lyrics still running after disable");
        });
        long before = context.computeOnClient(mc -> WorldText.drawnFrames());
        context.waitTicks(10);
        context.takeScreenshot("ly_6_module_off");
        long after = context.computeOnClient(mc -> WorldText.drawnFrames());
        if (after != before) throw new AssertionError("World lyrics drawn while the module is off");
        run(context, mc -> {
            ModuleManager.get().setEnabled("media", true);
            LyricsConfig.get().sky = "vanilla";
            Playback.play(demo());
        });

        guiScale(context, 2);
        context.setScreen(() -> new LyricsScreen(null));
        context.waitTicks(12);
        context.takeScreenshot("ly_7_menu");
        context.setScreen(() -> new AdvancedScreen(new LyricsScreen(null)));
        context.waitTicks(12);
        context.takeScreenshot("ly_8_advanced");
        context.setScreen(() -> new SearchScreen(new LyricsScreen(null), null));
        context.waitTicks(12);
        context.takeScreenshot("ly_9_search");
        context.setScreen(() -> null);
        guiScale(context, 0);
        run(context, mc -> {
            Playback.stop();
            hideHud(mc, false);
        });
    }

    private static Song demo() {
        Song demo = SongLibrary.find(SongLibrary.DEMO.toLowerCase(Locale.ROOT));
        if (demo == null) throw new AssertionError("Demo song missing from " + SongLibrary.songs());
        return demo;
    }

    private static void hideHud(Minecraft mc, boolean hidden) {
        mc.options.hideGui = hidden;
    }

    private static void guiScale(ClientGameTestContext context, int scale) {
        context.runOnClient(mc -> {
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
        });
    }

    private static void run(ClientGameTestContext context, Consumer<Minecraft> action) {
        context.runOnClient(action::accept);
    }

    private static void scene(ClientGameTestContext context, long ms, String name) {
        context.runOnClient(mc -> {
            Playback.seek(ms);
            Playback.pause();
        });
        context.waitTicks(10);
        context.takeScreenshot(name);
    }
}
