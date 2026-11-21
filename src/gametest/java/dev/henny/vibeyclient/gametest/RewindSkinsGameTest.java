package dev.henny.vibeyclient.gametest;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.launcher.LauncherStatus;
import dev.henny.vibeyclient.media.ffmpeg.Ffmpeg;
import dev.henny.vibeyclient.module.clips.ClipSaver;
import dev.henny.vibeyclient.module.clips.ClipsFiles;
import dev.henny.vibeyclient.module.clips.ClipsModule;
import dev.henny.vibeyclient.module.clips.Recorder;
import dev.henny.vibeyclient.module.clips.store.ClipStore;
import dev.henny.vibeyclient.module.skins.SkinSwap;
import dev.henny.vibeyclient.module.skins.SkinsModule;
import dev.henny.vibeyclient.module.skins.core.BridgeFiles;
import dev.henny.vibeyclient.module.skins.core.LibrarySkin;
import dev.henny.vibeyclient.module.skins.ui.SkinLibraryScreen;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

public final class RewindSkinsGameTest implements FabricClientGameTest {
    private static final String P = "[vibeyclient-rewind-skins-test] ";

    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            GlobalSettings.language.set(GlobalSettings.Language.GERMAN);
            Recorder.ignoreFocusForTest = true;
            if (clips().longRewind.on()) throw new AssertionError("long rewind must be off by default");
            if (clips().rewindMinutes.get() != 5 || clips().diskCap.get() != 1024) throw new AssertionError("rewind defaults");
            if (skins() == null || !skins().isEnabled()) throw new AssertionError("skins module should be on");
        });
        Path vibe = context.computeOnClient(mc -> LauncherStatus.vibeDir());
        boolean madeVibe = !Files.isDirectory(vibe);
        try {
            Files.createDirectories(vibe);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        try (TestSingleplayerContext world = context.worldBuilder()
                .setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeyrewind"))
                .create()) {
            world.getClientWorld().waitForChunksRender();
            int[] p = context.computeOnClient(mc -> new int[] {(int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getZ())});
            int x = p[0], z = p[1], y = 120;
            cmd(world, "gamemode creative @a");
            cmd(world, "time set 6000");
            cmd(world, "weather clear");
            cmd(world, "gamerule doDaylightCycle false");
            cmd(world, "fill %d %d %d %d %d %d minecraft:air", x - 8, y + 1, z - 8, x + 8, y + 8, z + 8);
            cmd(world, "fill %d %d %d %d %d %d minecraft:grass_block", x - 8, y, z - 8, x + 8, y, z + 8);
            cmd(world, "tp @a %.1f %d %.1f 0 10", x + 0.5, y + 1, z + 0.5);
            context.waitTicks(40);
            world.getClientWorld().waitForChunksRender();
            if (context.computeOnClient(mc -> Ffmpeg.available())) rewind(context);
            else System.out.println(P + "no FFmpeg: rewind part skipped");
            skinSwap(context, vibe);
        } finally {
            if (madeVibe) deleteTree(vibe);
            else cleanSkinFiles(vibe);
        }
    }

    private static void rewind(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> {
            ClipsModule m = clips();
            m.lowFpsPause.set(false);
            m.length.set(10);
            m.rewindMinutes.set(1);
            m.longRewind.set(true);
            m.buffer.set(true);
        });
        waitFor(ctx, "recording on disk", 600, () -> clips().recorder().state() == Recorder.State.RECORDING);
        ctx.runOnClient(mc -> dev.henny.vibeyclient.ui.toast.ToastStack.dismiss("clips.hint"));
        if (!ctx.computeOnClient(mc -> clips().recorder().ring().onDisk())) throw new AssertionError("ring should be on disk");
        ctx.waitTicks(20 * 16);
        long files = ctx.computeOnClient(mc -> countTs(ClipsFiles.bufferRoot()));
        long ramHeld = ctx.computeOnClient(mc -> clips().recorder().ring().bytes());
        System.out.println(P + "buffer files " + files + ", " + ramHeld / 1024 + " KB held, " + clips().recorder().ring().durationMs() + " ms");
        if (files < 5) throw new AssertionError("expected buffer files on disk, got " + files);
        ModulesPanel.preselect("clips");
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(10);
        ctx.takeScreenshot("rewind_01_page_de");
        ctx.setScreen(() -> null);
        ctx.waitTicks(10);
        int before = ClipsFiles.store().list().size();
        ctx.getInput().pressKey(clips().saveKey.mapping());
        waitFor(ctx, "rewind clip", 1200, () -> !ClipSaver.saving() && ClipsFiles.store().list().size() > before);
        ctx.waitTicks(4);
        ctx.takeScreenshot("rewind_02_saved");
        ClipStore.Clip c = ClipsFiles.store().list().get(0);
        JsonObject j = c.json();
        System.out.println(P + "rewind clip " + c.id() + " " + c.durationMs() + " ms: keyframes "
                + j.getAsJsonArray("keyframes_ms").size() + ", hint " + j.get("trim_hint") + ", cuts " + j.get("cuts_ms"));
        if (!j.has("rewind") || !j.get("rewind").getAsBoolean()) throw new AssertionError("rewind flag missing: " + j);
        if (c.durationMs() < 12_000) throw new AssertionError("whole rewind expected, got " + c.durationMs());
        if (j.getAsJsonArray("keyframes_ms").size() < 5) throw new AssertionError("keyframes missing");
        JsonObject hint = j.getAsJsonObject("trim_hint");
        long in = hint.get("in_ms").getAsLong(), out = hint.get("out_ms").getAsLong();
        if (out != c.durationMs() || in <= 0 || out - in < 10_000 || out - in > 13_000) throw new AssertionError("trim hint: " + hint);
        ctx.runOnClient(mc -> clips().longRewind.set(false));
        waitFor(ctx, "recording in RAM again", 600, () -> {
            ClipsFiles.drain(50);
            Recorder r = clips().recorder();
            return r.state() == Recorder.State.RECORDING && r.ring() != null && !r.ring().onDisk() && countTs(ClipsFiles.bufferRoot()) == 0;
        });
        ctx.runOnClient(mc -> {
            clips().buffer.set(false);
            clips().length.set(30);
            clips().rewindMinutes.set(5);
            clips().lowFpsPause.set(true);
        });
        ctx.waitTicks(20);
    }

    private static void skinSwap(ClientGameTestContext ctx, Path vibe) {
        UUID me = ctx.computeOnClient(mc -> mc.getUser().getProfileId());
        writeSkin(vibe, "aaaaaaaaaaaa.png", 64, 0xFF2E7D32, 0xFFFFC107);
        writeSkin(vibe, "bbbbbbbbbbbb.png", 32, 0xFF6A1B9A, 0xFF00BCD4);
        writeSkin(vibe, "cccccccccccc.png", 64, 0xFFB71C1C, 0xFFEEEEEE);
        writeLibrary(vibe, me.toString(), true, "aaaaaaaaaaaa");
        ctx.runOnClient(mc -> skins().swap().refresh());
        ctx.setScreen(() -> new SkinLibraryScreen(null));
        waitFor(ctx, "library", 100, () -> skins().swap().availability() == SkinSwap.Availability.READY);
        ctx.waitTicks(20);
        ctx.takeScreenshot("skins_01_library_de");
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
        ctx.waitTicks(5);
        ctx.takeScreenshot("skins_02_library_en");
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.GERMAN));

        String id = apply(ctx, "bbbbbbbbbbbb");
        String req = read(vibe.resolve(BridgeFiles.REQUEST));
        if (!req.contains("\"skin_id\":\"bbbbbbbbbbbb\"") || !req.contains("\"variant\":\"slim\"")) throw new AssertionError("request: " + req);
        result(vibe, "{\"schema\":1,\"id\":\"" + id + "\",\"status\":\"working\"}");
        waitFor(ctx, "working", 60, () -> skins().swap().phase() == SkinSwap.Phase.WORKING);
        ctx.waitTicks(4);
        ctx.takeScreenshot("skins_03_working");
        result(vibe, "{\"schema\":1,\"id\":\"" + id + "\",\"status\":\"ok\",\"skin_id\":\"bbbbbbbbbbbb\",\"variant\":\"slim\"}");
        waitFor(ctx, "done", 60, () -> skins().swap().phase() == SkinSwap.Phase.DONE);
        ctx.waitTicks(4);
        PlayerSkin skin = ctx.computeOnClient(mc -> mc.player.getSkin());
        System.out.println(P + "skin after swap: " + skin);
        if (!skin.body().texturePath().getPath().startsWith("skins/local_")) throw new AssertionError("local texture expected: " + skin);
        if (skin.model() != PlayerModelType.SLIM) throw new AssertionError("slim model expected: " + skin.model());
        ctx.takeScreenshot("skins_04_done_de");
        ctx.setScreen(() -> null);
        ctx.runOnClient(mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.player.setYRot(0);
            mc.player.setXRot(10);
        });
        ctx.waitTicks(20);
        ctx.takeScreenshot("skins_05_third_person");

        ctx.setScreen(() -> new SkinLibraryScreen(null));
        ctx.waitTicks(5);
        String id2 = apply(ctx, "cccccccccccc");
        result(vibe, "{\"schema\":1,\"id\":\"" + id2 + "\",\"status\":\"error\",\"error\":\"rate_limited\"}");
        waitFor(ctx, "failed", 60, () -> skins().swap().phase() == SkinSwap.Phase.FAILED);
        ctx.waitTicks(4);
        ctx.takeScreenshot("skins_06_rate_limited");
        String why = ctx.computeOnClient(mc -> skins().swap().failure());
        if (why == null || !why.toLowerCase(Locale.ROOT).contains("minute")) throw new AssertionError("rate limit text: " + why);

        String id3 = apply(ctx, "cccccccccccc");
        waitFor(ctx, "no answer", 400, () -> skins().swap().phase() == SkinSwap.Phase.FAILED);
        ctx.waitTicks(10);
        if (Files.exists(vibe.resolve(BridgeFiles.REQUEST))) throw new AssertionError("unanswered request should be withdrawn (" + id3 + ")");
        String skinAfter = ctx.computeOnClient(mc -> mc.player.getSkin().model().name());
        if (!"SLIM".equals(skinAfter)) throw new AssertionError("failed swaps keep the last skin");

        writeLibrary(vibe, UUID.randomUUID().toString(), true, null);
        waitFor(ctx, "wrong account", 100, () -> {
            skins().swap().refresh();
            return skins().swap().availability() == SkinSwap.Availability.WRONG_ACCOUNT;
        });
        ctx.waitTicks(5);
        ctx.takeScreenshot("skins_07_wrong_account");
        writeLibrary(vibe, me.toString(), false, null);
        waitFor(ctx, "offline account", 100, () -> {
            skins().swap().refresh();
            return skins().swap().availability() == SkinSwap.Availability.CANNOT_CHANGE;
        });
        ctx.waitTicks(5);
        ctx.takeScreenshot("skins_08_offline");
        ctx.setScreen(() -> null);
        ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        ctx.waitTicks(5);
    }

    private static String apply(ClientGameTestContext ctx, String skinId) {
        ctx.runOnClient(mc -> {
            LibrarySkin s = skins().swap().library().find(skinId);
            if (s == null || !skins().swap().apply(s)) throw new AssertionError("apply refused for " + skinId);
        });
        waitFor(ctx, "request file", 40, () -> Files.exists(LauncherStatus.vibeDir().resolve(BridgeFiles.REQUEST)));
        String req = read(LauncherStatus.vibeDir().resolve(BridgeFiles.REQUEST));
        int i = req.indexOf("\"id\":\"") + 6;
        return req.substring(i, req.indexOf('"', i));
    }

    private static void writeLibrary(Path vibe, String uuid, boolean canChange, String active) {
        String act = active == null ? "" : "\"active\":{\"library_id\":\"" + active + "\",\"variant\":\"classic\"},";
        String json = "{\"schema\":1,\"updated_at\":1795040000,\"account\":{\"uuid\":\"" + uuid + "\",\"name\":\"Tester\",\"kind\":\""
                + (canChange ? "microsoft" : "offline") + "\"},\"can_change\":" + canChange + ","
                + (canChange ? "" : "\"reason\":\"offline_account\",") + act
                + "\"skins\":["
                + "{\"id\":\"aaaaaaaaaaaa\",\"name\":\"Wald\",\"variant\":\"classic\",\"file\":\"aaaaaaaaaaaa.png\",\"sha1\":\"a\",\"added_at\":3},"
                + "{\"id\":\"bbbbbbbbbbbb\",\"name\":\"Alt (64×32)\",\"variant\":\"slim\",\"file\":\"bbbbbbbbbbbb.png\",\"sha1\":\"b\",\"added_at\":2},"
                + "{\"id\":\"cccccccccccc\",\"name\":\"Rot\",\"variant\":\"classic\",\"file\":\"cccccccccccc.png\",\"sha1\":\"c\",\"added_at\":1}]}";
        write(vibe.resolve(BridgeFiles.LIBRARY), json);
    }

    private static void writeSkin(Path vibe, String file, int h, int body, int face) {
        BufferedImage img = new BufferedImage(64, h, BufferedImage.TYPE_INT_ARGB);
        for (int yy = 0; yy < h; yy++) for (int xx = 0; xx < 64; xx++) {
            boolean overlay = (xx >= 32 && yy < 16) || (h == 64 && yy >= 32 && yy < 48) || (h == 64 && yy >= 48 && (xx < 16 || xx >= 48));
            int c = overlay ? 0 : (yy < 16 ? face : body);
            if (!overlay && ((xx + yy) % 7 == 0)) c = 0xFF101010;
            img.setRGB(xx, yy, c);
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            Path dir = vibe.resolve(BridgeFiles.SKIN_DIR);
            Files.createDirectories(dir);
            Files.write(dir.resolve(file), out.toByteArray());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static void result(Path vibe, String json) {
        write(vibe.resolve(BridgeFiles.RESULT), json);
        try {
            Files.deleteIfExists(vibe.resolve(BridgeFiles.REQUEST));
        } catch (IOException ignored) {
        }
    }

    private static void write(Path p, String s) {
        try {
            Path tmp = p.resolveSibling(p.getFileName() + ".tmp");
            Files.writeString(tmp, s, StandardCharsets.UTF_8);
            Files.move(tmp, p, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static String read(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static long countTs(Path root) {
        if (!Files.isDirectory(root)) return 0;
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(f -> f.toString().endsWith(".ts")).count();
        } catch (IOException e) {
            return -1;
        }
    }

    private static void cleanSkinFiles(Path vibe) {
        for (String f : List.of(BridgeFiles.LIBRARY, BridgeFiles.REQUEST, BridgeFiles.RESULT)) {
            try {
                Files.deleteIfExists(vibe.resolve(f));
            } catch (IOException ignored) {
            }
        }
        deleteTree(vibe.resolve(BridgeFiles.SKIN_DIR));
    }

    private static void deleteTree(Path p) {
        if (!Files.exists(p)) return;
        try (Stream<Path> s = Files.walk(p)) {
            for (Path q : s.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(q);
        } catch (IOException ignored) {
        }
    }

    private static void waitFor(ClientGameTestContext ctx, String what, int maxTicks, BooleanSupplier ok) {
        for (int i = 0; i < maxTicks; i++) {
            if (ctx.computeOnClient(mc -> ok.getAsBoolean())) return;
            ctx.waitTicks(1);
        }
        String status = ctx.computeOnClient(mc -> clips().recorder().state() + " " + clips().recorder().failure() + " | skins "
                + skins().swap().phase() + " " + skins().swap().availability() + " " + skins().swap().failure());
        throw new AssertionError("timed out waiting for " + what + " (" + status + ")");
    }

    private static void cmd(TestSingleplayerContext world, String format, Object... args) {
        world.getServer().runCommand(args.length == 0 ? format : String.format(Locale.ROOT, format, args));
    }

    private static ClipsModule clips() {
        return ModuleManager.get().get(ClipsModule.class);
    }

    private static SkinsModule skins() {
        return ModuleManager.get().get(SkinsModule.class);
    }
}
