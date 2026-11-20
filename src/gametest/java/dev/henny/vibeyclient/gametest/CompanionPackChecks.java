package dev.henny.vibeyclient.gametest;

import dev.henny.companio.mc.CompanioMc;
import dev.henny.companio.mc.client.CompanioClient;
import dev.henny.companio.mc.screen.PackRow;
import dev.henny.companio.mc.screen.PacksController;
import dev.henny.companio.pack.Pack;
import dev.henny.vui.mc.host.VuiScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static dev.henny.vibeyclient.gametest.CompanionTestKit.*;

final class CompanionPackChecks {
    private static final String MODEL = """
            {"format":"vibey","formatVersion":1,"kind":"cosmetic","id":"test:blob","name":{"en":"Blob"},"author":"t","tags":[],
             "rarity":"common","cosmetic":{"slot":"pet","pet":{"follow":"ground","scale":1}},
             "textures":[{"id":"main","width":1,"height":1,"png":"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="}],
             "model":{"texture":"main","bones":[{"name":"body","parent":null,"attach":"world","pivot":[0,0,0],
               "cubes":[{"from":[-4,0,-4],"to":[4,8,4],"uv":{"box":[0,0]}}]}]},
             "animations":{"idle":{"length":1,"loop":true,"bones":{}}}}""";

    private CompanionPackChecks() {}

    static void menu(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        tp(world, ctx, o[0] + 0.5, o[1], o[2] + 0.5, 0, 20);
        waitFor(ctx, "seal near", 200, () -> CompanioMc.own().body.distance(o[0] + 0.5, o[1], o[2] + 0.5) < 4.6
                && CompanioMc.own().body.speed() < 0.2);
        lookAt(ctx, center(ctx));
        ctx.getInput().pressMouse(RIGHT);
        ctx.waitTicks(6);
        String doc = ctx.computeOnClient(mc -> mc.screen instanceof VuiScreen s ? s.documentId() : "");
        check("companio:radial".equals(doc), "right click opens the companion menu, got " + doc);
        String errors = ctx.computeOnClient(mc -> ((VuiScreen) mc.screen).document().diagnostics().format());
        check(errors.isBlank(), "radial menu errors: " + errors);
        shot(ctx, "menu");
        ctx.getInput().pressKey(ESCAPE);
        ctx.waitTicks(4);
    }

    static void packScreen(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> CompanioMc.openPacks());
        ctx.waitTicks(8);
        String errors = ctx.computeOnClient(mc -> ((VuiScreen) mc.screen).document().diagnostics().format());
        check(errors.isBlank(), "pack screen errors: " + errors);
        shot(ctx, "pack_screen");
        ctx.getInput().pressKey(ESCAPE);
        ctx.waitTicks(4);
    }

    static void invalidPack(ClientGameTestContext ctx) {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("companionpacks").resolve("broken_test");
        try {
            write(dir);
            ctx.runOnClient(mc -> CompanioMc.reload());
            waitFor(ctx, "broken pack loaded", 200, () -> CompanioClient.get().registry().all().stream()
                    .anyMatch(p -> p.location().contains("broken_test") && p.errors() > 0));
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> CompanioClient.get().overlay().showing()), "error overlay shows");
            shot(ctx, "error_overlay");
            ctx.runOnClient(mc -> CompanioMc.openPacks());
            ctx.waitTicks(6);
            ctx.runOnClient(mc -> {
                PacksController c = (PacksController) ((VuiScreen) mc.screen).controller();
                for (PackRow r : c.available.snapshot()) if (r.name().contains("Broken")) c.select(r);
            });
            ctx.waitTicks(6);
            String first = ctx.computeOnClient(mc -> {
                Pack p = CompanioClient.get().registry().all().stream().filter(x -> x.location().contains("broken_test")).findFirst().orElseThrow();
                return p.problems().get(0).format();
            });
            log("broken pack: %s", first);
            check(first.contains("companions/blob.json"), "error names the file: " + first);
            shot(ctx, "pack_errors");
            ctx.getInput().pressKey(ESCAPE);
            ctx.waitTicks(4);
        } finally {
            delete(dir);
            ctx.runOnClient(mc -> CompanioMc.reload());
            ctx.waitTicks(20);
        }
    }

    private static void write(Path dir) {
        try {
            Files.createDirectories(dir.resolve("companions"));
            Files.createDirectories(dir.resolve("models"));
            Files.writeString(dir.resolve("models/blob.vibey"), MODEL);
            Files.writeString(dir.resolve("companion.json"), """
                    {"format":"companio-pack","formatVersion":1,"id":"broken_test","name":"Broken test pack",
                     "description":"Has mistakes on purpose","companions":["companions/blob.json"]}""");
            Files.writeString(dir.resolve("companions/blob.json"), """
                    {"id":"blob","name":"Blob","model":"models/blob.vibey",
                     "behaviors":[{"id":"rain","priority":5,"when":"wether.rain","prioirty":2}]}""");
        } catch (IOException e) {
            throw new AssertionError("write test pack", e);
        }
    }

    private static void delete(Path dir) {
        if (!Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
        }
    }
}
